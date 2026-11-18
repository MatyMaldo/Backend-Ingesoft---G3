package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Confirmación del preview de matrículas. Las filas se revalidan contra la base
 * antes de crear nada.
 */
public record ConfirmarCargaMasivaMatriculasRequest(
        @NotNull(message = "El año académico es obligatorio.")
        Long anioAcademicoId,

        @NotNull(message = "La lista de filas es obligatoria.")
        @Valid
        List<FilaConfirmacion> filas
) {

    public record FilaConfirmacion(
            String previewId,
            Long estudianteId,
            Long anioAcademicoId,
            Long gradoId,
            Long seccionId,
            String origen
    ) {
    }
}
