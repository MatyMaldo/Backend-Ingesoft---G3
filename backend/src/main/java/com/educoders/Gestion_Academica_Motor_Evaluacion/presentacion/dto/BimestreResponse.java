package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.NombreBimestre;

import java.time.LocalDate;

public record BimestreResponse(
        Long id,
        NombreBimestre nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {
}
