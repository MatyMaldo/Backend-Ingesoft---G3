package com.educoders.Seguridad_Auditoria.presentacion.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DetalleCursoEstudianteResponse(
        EstudianteInfo estudiante,
        CursoInfo curso,
        String gradoNombre,
        String seccionNombre,
        String docenteNombre,
        AnioAcademicoInfo anioConsulta,
        List<BimestreInfo> bimestres,
        List<CompetenciaInfo> competencias,
        List<EvaluacionInfo> evaluaciones
) {
    public record EstudianteInfo(
            Long id,
            String nombre,
            String codigo
    ) {}

    public record CursoInfo(
            Long id,
            String nombre
    ) {}

    public record AnioAcademicoInfo(
            Long id,
            Integer anio
    ) {}

    public record BimestreInfo(
            Long id,
            String nombre
    ) {}

    public record CompetenciaInfo(
            Long id,
            String nombre,
            Map<String, String> notasPorBimestre
    ) {}

    public record EvaluacionInfo(
            Long id,
            String nombre,
            String descripcion,
            LocalDate fecha,
            String bimestreNombre,
            String nota
    ) {}
}
