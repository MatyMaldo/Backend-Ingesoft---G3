package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.Mes;
import jakarta.validation.constraints.NotNull;

public record EmitirCompromisosPensionLoteRequest(
        @NotNull(message = "El año académico es obligatorio.")
        Long anioId,

        @NotNull(message = "El mes es obligatorio.")
        Mes mes,

        Long gradoId,
        Long seccionId
) {
}
