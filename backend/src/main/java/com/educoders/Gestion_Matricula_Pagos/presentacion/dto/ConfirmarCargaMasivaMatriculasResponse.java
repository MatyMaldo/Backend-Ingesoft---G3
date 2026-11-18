package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import java.util.List;

/**
 * Resultado de la confirmación de carga masiva. Si hay errores, no se crea
 * ninguna matrícula.
 */
public record ConfirmarCargaMasivaMatriculasResponse(
        int creados,
        List<ErrorConfirmacion> errores
) {

    public ConfirmarCargaMasivaMatriculasResponse {
        errores = errores == null ? List.of() : List.copyOf(errores);
    }

    public record ErrorConfirmacion(
            String origen,
            String tipo,
            String mensaje
    ) {
    }
}
