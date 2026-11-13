package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.NombreBimestre;

import java.util.List;

public record ContextoDocenteResponse(
        AnioAcademicoInfo anioAcademico,
        List<AsignacionInfo> asignaciones,
        List<BimestreInfo> bimestres,
        List<SeccionInfo> secciones,
        List<CompetenciaInfo> competencias
) {
    public record AnioAcademicoInfo(
            Long id,
            Integer anio,
            String fechaInicio,
            String fechaFin
    ) {}

    public record AsignacionInfo(
            Long idAsignacionCurso,
            Long idCurso,
            String nombreCurso,
            Long idSeccion,
            String nombreSeccion,
            Long idGrado,
            String nombreGrado
    ) {}

    public record BimestreInfo(
            Long id,
            NombreBimestre nombre,
            String fechaInicio,
            String fechaFin,
            boolean finalizado
    ) {}

    public record SeccionInfo(
            Long id,
            String nombre,
            Long idGrado,
            String nombreGrado
    ) {}

    public record CompetenciaInfo(
            Long id,
            String nombre,
            Long idCurso
    ) {}
}
