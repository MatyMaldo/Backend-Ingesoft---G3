package com.educoders.asistencia.dto;

import com.educoders.shared.enums.EstadoAsistencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos requeridos para registrar una asistencia.
 */
public record RegistrarAsistenciaRequest(
        @NotBlank(message = "El nombre del estudiante es obligatorio")
        @Size(max = 120, message = "El nombre del estudiante no puede superar 120 caracteres")
        String nombreEstudiante,
        @NotNull(message = "El estado de asistencia es obligatorio")
        EstadoAsistencia estado) {
}
