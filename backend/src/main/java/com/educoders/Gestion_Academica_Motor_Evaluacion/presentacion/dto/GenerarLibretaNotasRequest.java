
package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * HU-EVAL-06: Solicitud para generar la libreta
 * de notas de un estudiante al finalizar un bimestre.
 */
public record GenerarLibretaNotasRequest(

        @NotNull(message = "Debe seleccionar un bimestre.")
        Long idBimestre,

        @NotBlank(message = "Los comentarios del tutor son obligatorios.")
        @Size(
                max = 2000,
                message = "Los comentarios no pueden superar los 2000 caracteres."
        )
        String comentariosFinalesTutor
) {
}
