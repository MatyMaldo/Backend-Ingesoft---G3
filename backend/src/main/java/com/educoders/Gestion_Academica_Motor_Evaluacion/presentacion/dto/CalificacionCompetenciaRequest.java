
package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EscalaNota;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Representa la calificación final de una competencia.
 * HU-EVAL-07: el docente asigna una nota cualitativa
 * y una conclusión descriptiva al finalizar el bimestre.
 */
public record CalificacionCompetenciaRequest(

        @NotNull(message = "El ID de la competencia es obligatorio")
        @Positive
        Long idCompetencia,

        @NotNull(message = "La nota es obligatoria. Valores: AD, A, B, C")
        EscalaNota valorNota,

        @NotBlank(message = "La conclusión descriptiva es obligatoria.")
        @Size(
                max = 120,
                message = "La conclusión descriptiva no puede superar los 120 caracteres."
        )
        String conclusionDescriptiva
) {
    public CalificacionCompetenciaRequest {
        if (conclusionDescriptiva != null) {
            conclusionDescriptiva = conclusionDescriptiva.trim();
        }
    }
}
