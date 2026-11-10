package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import java.util.List;

public record ConfirmarCargaMasivaResponse(
        int bloquesCreados,
        List<ErrorConfirmacion> errores
) {
    public ConfirmarCargaMasivaResponse {
        errores = List.copyOf(errores);
    }

    public record ErrorConfirmacion(
            String origen,
            String tipo,
            String mensaje
    ) {
    }
}
