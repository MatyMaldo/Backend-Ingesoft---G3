package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lee la plantilla de matrículas en CSV o XLSX.
 * <p>
 * Columnas obligatorias: DNI, Código, Año, Grado y Sección. El orden no importa
 * y se admiten columnas adicionales. El año es solo informativo: el año destino
 * lo define el endpoint.
 */
@Component
public class MatriculaArchivoParser {

    public static final int MAX_FILAS = 1000;

    private static final List<String> COLUMNAS = List.of(
            "dni", "codigo", "anio", "grado", "seccion");

    public List<FilaMatricula> leer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Debe seleccionar un archivo no vacío.");
        }

        String nombre = nombre(archivo).toLowerCase(Locale.ROOT);
        try {
            if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
                return leerExcel(archivo);
            }
            if (nombre.endsWith(".csv")) {
                return leerCsv(archivo);
            }
            throw new ReglaNegocioException("Formato no soportado. Use CSV o XLSX.");
        } catch (ReglaNegocioException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new ReglaNegocioException("No se pudo leer el archivo: " + ex.getMessage());
        }
    }

    private List<FilaMatricula> leerCsv(MultipartFile archivo) throws IOException {
        String contenido = new String(archivo.getBytes(), StandardCharsets.UTF_8);
        if (contenido.startsWith("\uFEFF")) {
            contenido = contenido.substring(1);
        }

        String cabecera = Arrays.stream(contenido.split("\\r?\\n", -1))
                .filter(linea -> !linea.isBlank())
                .findFirst()
                .orElse("");

        if (cabecera.isBlank()) {
            throw new ReglaNegocioException("El archivo CSV está vacío.");
        }

        char separador = detectarSeparador(cabecera);
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(separador)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .get();

        try (CSVParser parser = formato.parse(new StringReader(contenido))) {

            List<FilaCruda> crudas = new ArrayList<>();
            for (CSVRecord registro : parser) {
                int numeroFila = Math.toIntExact(registro.getRecordNumber() + 1);
                if (!registro.isConsistent()) {
                    throw new ReglaNegocioException(
                            "Cantidad de columnas incorrecta en la fila " + numeroFila + ".");
                }
                String[] valores = new String[registro.size()];
                for (int i = 0; i < registro.size(); i++) {
                    valores[i] = registro.get(i);
                }
                crudas.add(new FilaCruda(numeroFila, valores));
            }
            return mapear(parser.getHeaderNames(), crudas);
        }
    }

    private List<FilaMatricula> leerExcel(MultipartFile archivo) throws IOException {
        try (InputStream entrada = archivo.getInputStream();
             Workbook workbook = WorkbookFactory.create(entrada)) {

            DataFormatter formatter = new DataFormatter();
            Sheet hoja = workbook.getSheetAt(0);

            int indiceCabecera = -1;
            for (int i = hoja.getFirstRowNum(); i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila != null && !filaVacia(fila, formatter)) {
                    indiceCabecera = i;
                    break;
                }
            }

            if (indiceCabecera < 0) {
                throw new ReglaNegocioException("El archivo Excel está vacío.");
            }

            Row cabecera = hoja.getRow(indiceCabecera);
            List<String> encabezados = new ArrayList<>();
            for (int c = 0; c < Math.max(cabecera.getLastCellNum(), 0); c++) {
                encabezados.add(formatter.formatCellValue(cabecera.getCell(c)));
            }

            List<FilaCruda> crudas = new ArrayList<>();
            for (int i = indiceCabecera + 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null || filaVacia(fila, formatter)) {
                    continue;
                }
                String[] valores = new String[encabezados.size()];
                for (int c = 0; c < valores.length; c++) {
                    valores[c] = formatter.formatCellValue(fila.getCell(c));
                }
                crudas.add(new FilaCruda(i + 1, valores));
            }
            return mapear(encabezados, crudas);
        }
    }

    private List<FilaMatricula> mapear(List<String> encabezados, List<FilaCruda> crudas) {
        Map<String, Integer> columnas = new HashMap<>();
        for (int i = 0; i < encabezados.size(); i++) {
            String clave = normalizar(encabezados.get(i));
            if (clave.isBlank()) {
                continue;
            }
            // "Año" normaliza a "ano"; el nombre interno es "anio".
            if ("ano".equals(clave)) {
                clave = "anio";
            }
            if (columnas.putIfAbsent(clave, i) != null) {
                throw new ReglaNegocioException(
                        "La columna '" + encabezados.get(i) + "' está repetida.");
            }
        }

        for (String requerida : COLUMNAS) {
            if (!columnas.containsKey(requerida)) {
                throw new ReglaNegocioException(
                        "El archivo debe tener las columnas: DNI, Código, Año, Grado, Sección.");
            }
        }

        List<FilaMatricula> filas = new ArrayList<>();
        for (FilaCruda cruda : crudas) {
            if (filas.size() >= MAX_FILAS) {
                throw new ReglaNegocioException(
                        "El archivo supera el límite de " + MAX_FILAS + " filas.");
            }
            filas.add(new FilaMatricula(
                    cruda.numero(),
                    valor(cruda.valores(), columnas, "dni"),
                    valor(cruda.valores(), columnas, "codigo"),
                    valor(cruda.valores(), columnas, "anio"),
                    valor(cruda.valores(), columnas, "grado"),
                    valor(cruda.valores(), columnas, "seccion")
            ));
        }

        if (filas.isEmpty()) {
            throw new ReglaNegocioException("El archivo no contiene filas para procesar.");
        }

        return filas;
    }

    private String valor(String[] valores, Map<String, Integer> columnas, String clave) {
        Integer indice = columnas.get(clave);
        if (indice == null || indice >= valores.length || valores[indice] == null) {
            return "";
        }
        return valores[indice].trim();
    }

    private boolean filaVacia(Row fila, DataFormatter formatter) {
        for (Cell celda : fila) {
            if (!formatter.formatCellValue(celda).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private char detectarSeparador(String cabecera) {
        long puntoYComa = cabecera.chars().filter(c -> c == ';').count();
        long coma = cabecera.chars().filter(c -> c == ',').count();
        if (puntoYComa == 0 && coma == 0) {
            throw new ReglaNegocioException("No se pudo detectar el separador del CSV.");
        }
        return puntoYComa >= coma ? ';' : ',';
    }

    /** Ignora mayúsculas, tildes y espacios repetidos al comparar nombres. */
    public static String normalizar(String valor) {
        String sinAcentos = Normalizer.normalize(
                        valor == null ? "" : valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String nombre(MultipartFile archivo) {
        return archivo.getOriginalFilename() == null
                ? "archivo"
                : archivo.getOriginalFilename();
    }

    public record FilaMatricula(
            int fila,
            String dni,
            String codigo,
            String anio,
            String grado,
            String seccion
    ) {
    }

    private record FilaCruda(int numero, String[] valores) {
    }
}
