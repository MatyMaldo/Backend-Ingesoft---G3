package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import java.util.List;

public record RevisionNotasResponse(
        Long idEstudiante,
        String nombreEstudiante,
        String nombreCurso,
        String nombreSeccion,
        List<EvaluacionNotaResponse> evaluaciones,
        List<NotaCompetenciaResponse> notasCompetencias
) {}
