package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;


public record VacantesAnioResponse(
        Long anioAcademicoId,
        Integer anioAcademico,
        Long capacidadTotal,
        Long matriculasOcupantes,
        Long vacantesDisponibles
){
}
