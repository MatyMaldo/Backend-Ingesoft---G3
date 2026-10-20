package com.educoders.Gestión_Academica_Motor_Evaluacion.presentacion.dto;

public record CompetenciaResponse(
        Long id,
        String nombre,
        String descripcion,
        Long cursoId
) {}