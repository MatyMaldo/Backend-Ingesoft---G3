package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarCursoRequest(
        @NotBlank(message = "El nombre del curso es obligatorio.")
        @Size(max = 120)
        String nombre,

        @Size(max = 500)
        String descripcion
) {}