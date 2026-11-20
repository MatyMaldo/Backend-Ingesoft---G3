package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.NombreBimestre;

import java.time.LocalDate;
import java.util.List;

/**
 * HU-EVAL-06: Contexto del docente tutor para generación de libretas.
 * Proporciona estudiantes de sus secciones, bimestres finalizados
 * y estado de las libretas por estudiante y bimestre.
 */
public record ContextoTutorLibretasResponse(
        AnioAcademicoInfo anioAcademico,
        List<EstudianteInfo> estudiantes,
        List<BimestreInfo> bimestres
) {
    public record AnioAcademicoInfo(
            Long id,
            Integer anio,
            String fechaInicio,
            String fechaFin
    ) {}

    public record EstudianteInfo(
            Long idEstudiante,
            String codigo,
            String nombre,
            Long idSeccion,
            String nombreSeccion,
            List<EstadoLibretaInfo> estadoLibretas
    ) {}

    public record EstadoLibretaInfo(
            Long idBimestre,
            String nombreBimestre,
            boolean libretaGenerada,
            LocalDate fechaGeneracion
    ) {}

    public record BimestreInfo(
            Long id,
            NombreBimestre nombre,
            String fechaInicio,
            String fechaFin,
            boolean finalizado
    ) {}
}
