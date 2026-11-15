
package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import java.util.List;

/**
 * HU-EVAL-06: Información académica y de asistencia
 * que se muestra al tutor antes de generar la libreta.
 */
public record VistaPreviaLibretaNotasResponse(

        Long idEstudiante,
        String nombreEstudiante,

        Long idSeccion,
        String nombreSeccion,
        String nombreTutor,

        Long idBimestre,
        String nombreBimestre,
        Integer anioAcademico,
        String nombreInstitucion,

        long inasistencias,
        long tardanzas,

        List<LibretaNotasDetalleResponse.NotaPorCompetencia> notasCompetencias
) {
}
