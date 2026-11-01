package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EscalaNota;
import java.time.LocalDate;

public record NotaResponse(
        Long idNota,
        Long idEvaluacion,
        String nombreEvaluacion,
        Long idEstudiante,
        String nombreEstudiante,
        EscalaNota valorNota,
        LocalDate fechaRegistro
) {
}
