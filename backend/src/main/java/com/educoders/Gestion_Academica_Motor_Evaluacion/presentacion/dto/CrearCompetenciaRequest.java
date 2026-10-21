package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearCompetenciaRequest(

        @NotBlank(message = "El nombre de la competencia es obligatorio.")
        @Size(max = 180, message = "El nombre no puede superar 180 caracteres.")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria.") // Cumple Escenario 3
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres.")
        String descripcion
) {}