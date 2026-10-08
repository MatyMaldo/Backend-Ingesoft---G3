package com.educoders.Control_Asistencia.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;

/**
 * Datos de asistencia enviados al frontend.
 */
public record AsistenciaResponse(
        Long id,
        String nombreEstudiante,
        EstadoAsistencia estado) {
}
