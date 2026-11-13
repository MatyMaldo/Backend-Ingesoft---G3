package com.educoders.Gestion_Asistencia.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;

import java.time.LocalDateTime;

public record AsistenciaEstudianteDetalleResponse(
        Long estudianteId,
        String codigo,
        String nombreCompleto,
        EstadoAsistencia estado,
        String observaciones,
        LocalDateTime fechaHoraRegistro
) {
}
