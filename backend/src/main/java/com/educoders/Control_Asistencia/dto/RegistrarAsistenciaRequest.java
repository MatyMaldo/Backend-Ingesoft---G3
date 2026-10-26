package com.educoders.Control_Asistencia.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Datos requeridos para registrar una asistencia.
 */
public record RegistrarAsistenciaRequest(
        @NotNull(message = "La fecha de registro es obligatoria")
        LocalDate fechaRegistro,
        @NotNull(message = "El estado de asistencia es obligatorio")
        EstadoAsistencia estado,
        @NotNull(message = "El ID de la sesión de clase es obligatorio")
        Long sesionClaseId,
        @NotNull(message = "El ID del estudiante es obligatorio")
        Long estudianteId) {
}
