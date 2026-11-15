package com.educoders.Seguridad_Auditoria.presentacion.dto;

/**
 * Curso en el que está matriculado un estudiante, con contexto de grado, sección y año académico.
 */
public record CursoEstudianteResponse(
        Long id,
        String nombre,
        String gradoNombre,
        Long seccionId,
        String seccionNombre,
        AnioAcademicoInfo anioAcademico
) {
    public record AnioAcademicoInfo(
            Long id,
            Integer anio
    ) {}
}
