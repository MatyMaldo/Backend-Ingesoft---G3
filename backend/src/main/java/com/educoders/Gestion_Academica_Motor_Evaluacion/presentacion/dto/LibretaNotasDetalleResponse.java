
package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EscalaNota;

import java.time.LocalDate;
import java.util.List;

/**
 * HU-EVAL-04/05/06: Detalle de una libreta generada.
 * Incluye calificaciones, asistencia y datos académicos.
 */
public record LibretaNotasDetalleResponse(
        Long idLibreta,
        Long idEstudiante,
        Integer anioAcademico,
        String nombreBimestre,
        LocalDate fechaGeneracion,
        String comentariosFinalesTutor,
        List<NotaPorCompetencia> notasCompetencias,
        String nombreEstudiante,
        String nombreSeccion,
        String nombreTutor,
        String nombreInstitucion,
        long inasistencias,
        long tardanzas,
        List<ProgresoCompetenciaAcumuladoResponse> progresoAcumulado
) {

    public record NotaPorCompetencia(
            String nombreCurso,
            String nombreCompetencia,
            EscalaNota valorNota,
            String descripcion,
            String conclusionDescriptiva
    ) {
    }
}
