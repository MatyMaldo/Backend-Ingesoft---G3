package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EscalaNota;
import java.time.LocalDate;

public record EvaluacionNotaResponse(
        Long idEvaluacion,
        String nombreEvaluacion,
        String descripcion,
        LocalDate fecha,
        String nombreBimestre,
        String nombreCompetencia,
        EscalaNota valorNota // Puede ser null si el docente no ha registrado nota
) {}