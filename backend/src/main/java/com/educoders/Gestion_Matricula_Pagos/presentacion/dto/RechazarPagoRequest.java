package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RechazarPagoRequest(
        @NotNull(message = "El ID del validador es obligatorio")
        Long validadorId,

        @NotBlank(message = "El motivo de rechazo es obligatorio")
        String motivo
) {}