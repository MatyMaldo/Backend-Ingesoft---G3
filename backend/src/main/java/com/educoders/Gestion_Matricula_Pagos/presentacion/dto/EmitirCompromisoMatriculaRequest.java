package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EmitirCompromisoMatriculaRequest(
        @NotNull(message = "El identificador de la matrícula es obligatorio.")
        Long matriculaId,

        BigDecimal montoPersonalizado // Opcional: Si se envía null, el backend usa la tarifa preconfigurada
) {}