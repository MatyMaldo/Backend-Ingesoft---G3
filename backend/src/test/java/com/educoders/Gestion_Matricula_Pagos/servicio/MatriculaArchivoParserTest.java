package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatriculaArchivoParserTest {

    private final MatriculaArchivoParser parser = new MatriculaArchivoParser();

    private MockMultipartFile csv(String contenido) {
        return new MockMultipartFile(
                "archivo",
                "matriculas.csv",
                "text/csv",
                contenido.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void leeCsvConBomYSeparadorPuntoYComa() {
        String contenido = "\uFEFFDNI;Código;Año;Grado;Sección\r\n"
                + "12345678;EST-001;2027;1er Grado de Secundaria;A\r\n";

        List<MatriculaArchivoParser.FilaMatricula> filas = parser.leer(csv(contenido));

        assertEquals(1, filas.size());
        assertEquals(2, filas.get(0).fila());
        assertEquals("12345678", filas.get(0).dni());
        assertEquals("EST-001", filas.get(0).codigo());
        assertEquals("1er Grado de Secundaria", filas.get(0).grado());
        assertEquals("A", filas.get(0).seccion());
    }

    @Test
    void leeCsvConComaYOrdenDeColumnasDistinto() {
        String contenido = "Grado,Sección,DNI,Código,Año\r\n"
                + "1er Grado de Secundaria,A,12345678,EST-001,2027\r\n";

        List<MatriculaArchivoParser.FilaMatricula> filas = parser.leer(csv(contenido));

        assertEquals(1, filas.size());
        assertEquals("12345678", filas.get(0).dni());
        assertEquals("1er Grado de Secundaria", filas.get(0).grado());
    }

    @Test
    void csvSinColumnaObligatoria_rechaza() {
        String contenido = "DNI;Código;Año;Grado\r\n"
                + "12345678;EST-001;2027;1er Grado\r\n";

        assertThrows(ReglaNegocioException.class, () -> parser.leer(csv(contenido)));
    }

    @Test
    void leeExcelConCabecerasYFila() throws Exception {
        byte[] archivo;
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            Sheet hoja = workbook.createSheet("Matriculas");
            Row cabecera = hoja.createRow(0);
            cabecera.createCell(0).setCellValue("DNI");
            cabecera.createCell(1).setCellValue("Código");
            cabecera.createCell(2).setCellValue("Año");
            cabecera.createCell(3).setCellValue("Grado");
            cabecera.createCell(4).setCellValue("Sección");

            Row fila = hoja.createRow(1);
            fila.createCell(0).setCellValue("12345678");
            fila.createCell(1).setCellValue("EST-001");
            fila.createCell(2).setCellValue(2027);
            fila.createCell(3).setCellValue("1er Grado de Secundaria");
            fila.createCell(4).setCellValue("A");

            workbook.write(salida);
            archivo = salida.toByteArray();
        }

        MockMultipartFile excel = new MockMultipartFile(
                "archivo", "matriculas.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                archivo);

        List<MatriculaArchivoParser.FilaMatricula> filas = parser.leer(excel);

        assertEquals(1, filas.size());
        assertEquals("12345678", filas.get(0).dni());
        assertEquals("2027", filas.get(0).anio());
        assertEquals("A", filas.get(0).seccion());
    }
}
