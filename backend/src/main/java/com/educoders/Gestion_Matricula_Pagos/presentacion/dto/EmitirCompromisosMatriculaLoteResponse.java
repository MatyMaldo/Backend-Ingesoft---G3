package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import java.util.List;

public record EmitirCompromisosMatriculaLoteResponse(
        int emitidos,
        int omitidos,
        List<EmisionCompromisoLoteError> errores
) {
    public EmitirCompromisosMatriculaLoteResponse {
        errores = List.copyOf(errores);
    }
}
