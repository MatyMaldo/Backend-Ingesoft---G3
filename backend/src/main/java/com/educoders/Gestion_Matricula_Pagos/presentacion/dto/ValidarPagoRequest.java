package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import jakarta.validation.constraints.NotNull;

public record ValidarPagoRequest(
        @NotNull(message = "El ID del validador es obligatorio")
        Long validadorId
) {}