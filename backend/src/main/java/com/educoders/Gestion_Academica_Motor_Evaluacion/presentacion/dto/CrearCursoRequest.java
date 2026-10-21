package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearCursoRequest(
        @NotBlank(message = "El nombre del curso es obligatorio.")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres.")
        String descripcion
) {}
