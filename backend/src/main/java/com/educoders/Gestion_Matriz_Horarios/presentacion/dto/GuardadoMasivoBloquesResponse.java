package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.DiaSemana;

import java.time.LocalTime;
import java.util.List;

public record GuardadoMasivoBloquesResponse(
        List<BloqueMasivoItemResponse> bloques,
        int creados,
        int actualizados,
        int eliminados
) {
    public GuardadoMasivoBloquesResponse {
        bloques = List.copyOf(bloques);
    }

    public record BloqueMasivoItemResponse(
            Long id,
            Long asignacionCursoId,
            Long aulaId,
            DiaSemana diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
    }
}
