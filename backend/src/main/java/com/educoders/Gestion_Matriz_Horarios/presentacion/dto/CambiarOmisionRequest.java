package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarOmisionRequest(
        @NotNull(message = "Debe indicar si la sesión queda omitida.")
        Boolean omitida
) {
}
