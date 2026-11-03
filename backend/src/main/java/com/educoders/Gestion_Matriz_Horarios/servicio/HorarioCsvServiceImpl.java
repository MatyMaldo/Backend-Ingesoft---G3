package com.educoders.Gestion_Matriz_Horarios.servicio;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class HorarioCsvServiceImpl implements HorarioCsvService {

    /** Separador por defecto de la plantilla. */
    public static final String SEPARADOR = ";";

    /** Encabezados en el orden exigido por HU-GEMIH-08. */
    public static final String[] ENCABEZADOS = {
            "Grado", "Sección", "Curso", "DNI Docente", "Aula", "Día", "Hora inicio", "Hora fin"
    };

    /** Fila de ejemplo identificada con EJEMPLO (el admin debe eliminarla). */
    public static final String FILA_EJEMPLO =
            "EJEMPLO;A;Matemática;1234567;101;Lunes;08:00;09:00";

    @Override
    public byte[] generarPlantilla() {
        StringBuilder sb = new StringBuilder();
        // BOM para que Excel reconozca UTF-8 y respete tildes/ñ.
        sb.append('\uFEFF');
        sb.append(String.join(SEPARADOR, ENCABEZADOS)).append("\r\n");
        sb.append(FILA_EJEMPLO).append("\r\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}