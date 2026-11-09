package com.educoders.Seguridad_Auditoria.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;

/**
 * Estudiante vinculado al apoderado autenticado.
 *
 * Los campos de grado, sección y año solo se completan cuando se solicita
 * el listado con el parámetro opcional anioId.
 */
public record EstudianteApoderadoResponse(
        Long id,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        String dni,
        String codigo,
        Long gradoId,
        String gradoNombre,
        Long seccionId,
        String seccionNombre,
        Long anioAcademicoId,
        Integer anioAcademico,
        EstadoMatricula estadoMatricula
) {
}
