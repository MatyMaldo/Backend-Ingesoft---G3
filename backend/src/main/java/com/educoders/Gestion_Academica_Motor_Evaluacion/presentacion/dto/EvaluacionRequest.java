package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EvaluacionRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 160, message = "El nombre no debe exceder los 160 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 1000, message = "La descripción no debe exceder los 1000 caracteres")
        String descripcion,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "El ID de la competencia es obligatorio")
        Long idCompetencia,

        @NotNull(message = "El ID de la asignación de curso es obligatorio")
        Long idAsignacionCurso
) {
}
