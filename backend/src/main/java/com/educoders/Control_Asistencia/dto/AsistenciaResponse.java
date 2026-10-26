package com.educoders.Control_Asistencia.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;

import java.time.LocalDate;

/**
 * Datos de asistencia enviados al frontend.
 */
public record AsistenciaResponse(
        Long id,
        LocalDate fechaRegistro,
        EstadoAsistencia estado,
        Long sesionClaseId,
        Long estudianteId) {
}
