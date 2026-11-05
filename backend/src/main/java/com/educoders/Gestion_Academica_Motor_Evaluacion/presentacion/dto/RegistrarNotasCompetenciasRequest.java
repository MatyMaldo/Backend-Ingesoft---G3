
package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Solicitud para registrar las calificaciones finales
 * de las competencias de un curso para un estudiante
 * en un bimestre específico.
 */
public record RegistrarNotasCompetenciasRequest(

        @NotNull
        @Positive
        Long idAsignacionCurso,

        @NotNull
        @Positive
        Long idEstudiante,

        @NotNull
        @Positive
        Long idBimestre,

        @NotEmpty(message = "Debe registrar las competencias del curso.")
        List<@Valid CalificacionCompetenciaRequest> calificaciones
) {
}
