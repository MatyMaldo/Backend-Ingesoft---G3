package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoPeriodoMatricula;

import java.time.LocalDate;

/**
 * Estado del periodo de matrícula de un año académico y las ventanas
 * que habilitan cada parte del flujo de horarios.
 */
public record PeriodoMatriculaEstadoResponse(
        Long periodoId,
        Long anioAcademicoId,
        Integer anio,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoPeriodoMatricula estado,
        boolean asignacionCursoDocenteAbierta,
        boolean crudBloquesHorarioAbierta,
        boolean generacionSesionesAbierta
) {
}
