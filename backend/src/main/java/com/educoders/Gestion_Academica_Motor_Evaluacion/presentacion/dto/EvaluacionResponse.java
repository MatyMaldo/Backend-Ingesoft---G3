package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import java.time.LocalDate;

public record EvaluacionResponse(
        Long idEvaluacion,
        String nombre,
        String descripcion,
        LocalDate fecha,
        String nombreBimestre,
        String nombreCompetencia,
        String nombreCurso,
        String nombreSeccion
) {
}
