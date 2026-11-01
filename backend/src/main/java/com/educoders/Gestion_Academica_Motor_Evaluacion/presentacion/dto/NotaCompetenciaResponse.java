package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EscalaNota;

public record NotaCompetenciaResponse(
        String nombreCompetencia,
        String nombreBimestre,
        EscalaNota valorNota,
        String conclusionDescriptiva
) {}
