
package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EscalaNota;
import java.time.LocalDate;
import java.util.List;

public record LibretaNotasDetalleResponse(
        Long idLibreta,
        Long idEstudiante,
        Integer anioAcademico,
        String nombreBimestre,
        LocalDate fechaGeneracion,
        String comentariosFinalesTutor,
        List<NotaPorCompetencia> notasCompetencias
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
