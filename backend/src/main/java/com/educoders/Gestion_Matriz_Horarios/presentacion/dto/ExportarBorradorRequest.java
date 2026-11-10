package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Exporta una grilla de horarios que todavía no fue guardada.
 */
public record ExportarBorradorRequest(
        @NotNull(message = "El año académico es obligatorio.")
        Long anioAcademicoId,

        @NotNull(message = "La lista de bloques es obligatoria.")
        @Valid
        List<ConfirmarCargaMasivaRequest.BloqueConfirmacion> bloques,

        String formato
) {
}
