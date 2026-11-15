package com.educoders.Seguridad_Auditoria.presentacion.dto;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionNotaResponse;

import java.util.List;

public record EvaluacionesEstudianteResponse(
        Long idEstudiante,
        String nombreEstudiante,
        Long idCurso,
        String nombreCurso,
        List<EvaluacionNotaResponse> evaluaciones
) {}
