package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoPeriodoMatricula;

import java.util.List;

/** Estado: OK, SIN_BLOQUES o SIN_MATRICULA. */
public record HorarioSemanalResponse(
        String estado,
        String mensaje,
        EstadoPeriodoMatricula estadoPeriodo,
        List<BloqueHorarioResponse> bloques
) {
    public HorarioSemanalResponse {
        bloques = List.copyOf(bloques);
    }
}
