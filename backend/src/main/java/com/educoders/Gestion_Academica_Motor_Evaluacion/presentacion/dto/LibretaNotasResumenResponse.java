
package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import java.time.LocalDate;

public record LibretaNotasResumenResponse(
        Long idLibreta,
        Long idEstudiante,
        Integer anioAcademico,
        String nombreBimestre,
        LocalDate fechaGeneracion,
        String comentariosFinalesTutor
) {
}
