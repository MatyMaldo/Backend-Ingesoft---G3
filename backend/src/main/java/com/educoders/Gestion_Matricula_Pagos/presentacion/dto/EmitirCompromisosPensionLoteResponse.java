package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import java.util.List;

public record EmitirCompromisosPensionLoteResponse(
        int emitidos,
        int omitidos,
        List<EmisionCompromisoLoteError> errores
) {
    public EmitirCompromisosPensionLoteResponse {
        errores = List.copyOf(errores);
    }
}
