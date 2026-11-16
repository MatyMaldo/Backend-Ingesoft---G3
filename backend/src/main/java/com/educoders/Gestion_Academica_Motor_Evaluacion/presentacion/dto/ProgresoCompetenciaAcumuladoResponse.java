
package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EscalaNota;

/**
 * Representa el progreso acumulado de una competencia
 * durante los cuatro bimestres del año académico.
 *
 * Los periodos todavía no evaluados mantienen valor null.
 */
public record ProgresoCompetenciaAcumuladoResponse(
        Long idCompetencia,
        String nombreCurso,
        String nombreCompetencia,
        EscalaNota bimestre1,
        EscalaNota bimestre2,
        EscalaNota bimestre3,
        EscalaNota bimestre4
) {
}
