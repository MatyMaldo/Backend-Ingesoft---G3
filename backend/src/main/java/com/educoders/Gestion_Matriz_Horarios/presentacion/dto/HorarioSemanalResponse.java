package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import java.util.List;

/** Estado: OK, SIN_BLOQUES o SIN_MATRICULA. */
public record HorarioSemanalResponse(
        String estado,
        String mensaje,
        List<BloqueHorarioResponse> bloques
) {
    public HorarioSemanalResponse {
        bloques = List.copyOf(bloques);
    }
}
