package com.educoders.Gestion_Asistencia.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;

import java.time.LocalDateTime;

public record AsistenciaDocenteResponse(
        EstadoAsistencia estado,
        LocalDateTime fechaHoraRegistro
) {
}
