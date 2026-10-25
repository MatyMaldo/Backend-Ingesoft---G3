package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import java.util.List;

public record GradoConSeccionesResponse(
        Long gradoId,
        String gradoNombre,
        List<SeccionResponse> secciones,
        String estado
) {
}