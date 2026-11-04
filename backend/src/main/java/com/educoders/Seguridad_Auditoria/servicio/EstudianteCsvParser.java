
package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee y valida archivos CSV para el registro masivo de estudiantes.
 * HU-GEMAP-02: carga masiva de estudiantes.
 */
@Component
public class EstudianteCsvParser {

    private static final int MAX_ESTUDIANTES = 1000;

    private static final List<String> ENCABEZADOS = List.of(
            "nombre",
            "apellidoPaterno",
            "apellidoMaterno",
            "dni",
            "email",
            "telefono",
            "fechaNacimiento",
            "apoderadoId"
    );

    public List<FilaEstudianteCsv> leer(InputStream archivo) {

        List<FilaEstudianteCsv> estudiantes = new ArrayList<>();

        try (PushbackReader lector = new PushbackReader(
                new InputStreamReader(archivo, StandardCharsets.UTF_8), 1)) {

            // Permite leer archivos UTF-8 con o sin BOM.
            int primerCaracter = lector.read();
            if (primerCaracter != '\uFEFF' && primerCaracter != -1) {
                lector.unread(primerCaracter);
            }

            CSVFormat formato = CSVFormat.DEFAULT.builder()
                    .setDelimiter(';')
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .get();

            try (CSVParser parser = formato.parse(lector)) {

                if (!parser.getHeaderNames().equals(ENCABEZADOS)) {
                    throw new ReglaNegocioException(
                            "Los encabezados del CSV no son correctos.");
                }

                for (CSVRecord registro : parser) {

                    int numeroFila = Math.toIntExact(
                            registro.getRecordNumber() + 1);

                    if (estudiantes.size() >= MAX_ESTUDIANTES) {
                        throw new ReglaNegocioException(
                                "El CSV supera el límite de 1000 estudiantes.");
                    }

                    if (!registro.isConsistent()) {
                        throw new ReglaNegocioException(
                                "Cantidad de columnas incorrecta en fila "
                                        + numeroFila);
                    }

                    String nombre = obligatorio(
                            registro, "nombre", numeroFila);

                    String apellidoPaterno = obligatorio(
                            registro, "apellidoPaterno", numeroFila);

                    String dni = obligatorio(
                            registro, "dni", numeroFila);

                    String email = obligatorio(
                            registro, "email", numeroFila);

                    String fechaTexto = obligatorio(
                            registro, "fechaNacimiento", numeroFila);

                    String apoderadoTexto = obligatorio(
                            registro, "apoderadoId", numeroFila);

                    LocalDate fechaNacimiento;
                    Long apoderadoId;

                    try {
                        fechaNacimiento = LocalDate.parse(fechaTexto);
                        apoderadoId = Long.valueOf(apoderadoTexto);
                    } catch (DateTimeParseException | NumberFormatException ex) {
                        throw new ReglaNegocioException(
                                "Fecha o ID de apoderado inválido en fila "
                                        + numeroFila);
                    }

                    if (!dni.matches("\\d{8}")) {
                        throw new ReglaNegocioException(
                                "DNI inválido en fila " + numeroFila);
                    }

                    if (apoderadoId <= 0) {
                        throw new ReglaNegocioException(
                                "ID de apoderado inválido en fila "
                                        + numeroFila);
                    }

                    estudiantes.add(new FilaEstudianteCsv(
                            nombre,
                            apellidoPaterno,
                            registro.get("apellidoMaterno"),
                            dni,
                            email,
                            registro.get("telefono"),
                            fechaNacimiento,
                            apoderadoId
                    ));
                }
            }

            if (estudiantes.isEmpty()) {
                throw new ReglaNegocioException(
                        "El archivo CSV no contiene estudiantes.");
            }

            return estudiantes;

        } catch (IOException | UncheckedIOException ex) {
            throw new ReglaNegocioException(
                    "No se pudo leer el archivo CSV: " + ex.getMessage());
        }
    }

    private String obligatorio(
            CSVRecord registro, String columna, int fila) {

        String valor = registro.get(columna);

        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException(
                    "El campo " + columna
                            + " es obligatorio en fila " + fila);
        }

        return valor;
    }

    /**
     * Representa una fila validada estructuralmente del CSV.
     */
    public record FilaEstudianteCsv(
            String nombre,
            String apellidoPaterno,
            String apellidoMaterno,
            String dni,
            String email,
            String telefono,
            LocalDate fechaNacimiento,
            Long apoderadoId
    ) {
    }
}
