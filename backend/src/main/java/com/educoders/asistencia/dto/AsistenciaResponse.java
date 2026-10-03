package com.educoders.asistencia.dto;

import com.educoders.shared.enums.EstadoAsistencia;

/**
 * Datos de asistencia enviados al frontend.
 */
public record AsistenciaResponse(
        Long id,
        String nombreEstudiante,
        EstadoAsistencia estado) {
}
