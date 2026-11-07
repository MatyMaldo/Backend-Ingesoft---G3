package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GuardarBloquesSeccionRequest(
        @NotNull(message = "El año académico es obligatorio")
        Long anioAcademicoId,

        @NotNull(message = "La lista de bloques es obligatoria")
        @Valid
        List<BloqueMasivoItemRequest> bloques
) {
}
