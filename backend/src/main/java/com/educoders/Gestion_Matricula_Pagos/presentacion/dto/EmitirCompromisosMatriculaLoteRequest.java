package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import jakarta.validation.constraints.NotNull;

public record EmitirCompromisosMatriculaLoteRequest(
        @NotNull(message = "El año académico es obligatorio.")
        Long anioId
) {
}
