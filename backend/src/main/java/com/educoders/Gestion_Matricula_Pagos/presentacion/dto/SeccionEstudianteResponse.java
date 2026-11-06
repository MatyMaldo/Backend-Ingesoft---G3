package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;

/**
 * Grado y sección de un estudiante para un año académico concreto.
 */
public record SeccionEstudianteResponse(
        Long matriculaId,
        Long estudianteId,
        Long anioAcademicoId,
        Integer anio,
        EstadoMatricula estado,
        Long gradoId,
        String gradoNombre,
        Long seccionId,
        String seccionNombre
) {
}
