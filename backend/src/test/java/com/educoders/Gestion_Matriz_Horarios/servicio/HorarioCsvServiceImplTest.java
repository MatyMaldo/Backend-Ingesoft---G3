package com.educoders.Gestion_Matriz_Horarios.servicio;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorarioCsvServiceImplTest {

    private final HorarioCsvServiceImpl service = new HorarioCsvServiceImpl();

    private String comoTexto(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Test
    void generarPlantilla_incluyeBomUtf8() {
        byte[] bytes = service.generarPlantilla();

        assertTrue(bytes.length >= 3);
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
    }

    @Test
    void generarPlantilla_encabezadosEnOrden() {
        String texto = comoTexto(service.generarPlantilla());
        String sinBom = texto.startsWith("\uFEFF") ? texto.substring(1) : texto;
        String encabezado = sinBom.split("\\r?\\n")[0];

        assertEquals("Grado;Sección;Curso;DNI Docente;Aula;Día;Hora inicio;Hora fin", encabezado);
    }

    @Test
    void generarPlantilla_usaSeparadorPuntoYComa() {
        String texto = comoTexto(service.generarPlantilla());
        String encabezado = texto.replace("\uFEFF", "").split("\\r?\\n")[0];

        // 8 columnas -> 7 separadores
        assertEquals(7, encabezado.chars().filter(c -> c == ';').count());
    }

    @Test
    void generarPlantilla_incluyeFilaEjemplo() {
        String texto = comoTexto(service.generarPlantilla());
        String[] lineas = texto.replace("\uFEFF", "").split("\\r?\\n");

        assertTrue(lineas.length >= 2);
        assertEquals("EJEMPLO;A;Matemática;1234567;101;Lunes;08:00;09:00", lineas[1]);
    }
}