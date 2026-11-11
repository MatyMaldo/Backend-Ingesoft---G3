package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;

public record EstudianteAsignacionResponse(
        Long idEstudiante,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        EstadoMatricula estadoMatricula
) {}
