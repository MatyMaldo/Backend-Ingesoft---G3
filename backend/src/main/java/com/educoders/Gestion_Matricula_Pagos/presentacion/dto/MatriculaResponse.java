package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import java.time.LocalDate;

public record MatriculaResponse(
        Long id,
        LocalDate fechaAsignacion,
        EstadoMatricula estado,
        Long estudianteId,
        String nombreCompletoEstudiante,
        String codigoEstudiante,
        Long seccionId,
        String nombreSeccion,
        String nombreGrado,
        Integer anioAcademico
) {}