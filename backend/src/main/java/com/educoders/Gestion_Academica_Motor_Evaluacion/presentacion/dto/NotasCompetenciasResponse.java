
package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EscalaNota;

import java.util.List;

/**
 * Respuesta del registro de calificaciones finales
 * por competencia, estudiante y bimestre.
 */
public record NotasCompetenciasResponse(
        Long idAsignacionCurso,
        Long idEstudiante,
        Long idBimestre,
        List<NotaCompetenciaRegistrada> notasCompetencias
) {

    /**
     * Representa una calificación de competencia registrada.
     */
    public record NotaCompetenciaRegistrada(
            Long idNotaCompetencia,
            Long idCompetencia,
            EscalaNota valorNota,
            String conclusionDescriptiva
    ) {
    }
}
