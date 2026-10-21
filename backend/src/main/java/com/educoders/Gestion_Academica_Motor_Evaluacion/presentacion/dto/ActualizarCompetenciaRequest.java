package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarCompetenciaRequest(

        @NotBlank(message = "El nombre de la competencia es obligatorio.")
        @Size(max = 180)
        String nombre,

        @NotBlank(message = "La descripción es obligatoria.")
        @Size(max = 1000)
        String descripcion
) {}
