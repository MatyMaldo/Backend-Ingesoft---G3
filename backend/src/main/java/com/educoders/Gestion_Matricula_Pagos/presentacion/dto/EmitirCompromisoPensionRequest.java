package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.Mes;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EmitirCompromisoPensionRequest(
        @NotNull(message = "El identificador de la matrícula es obligatorio.")
        Long matriculaId,

        @NotNull(message = "El mes correspondiente a la pensión es obligatorio.")
        Mes mes,

        BigDecimal montoPersonalizado // Opcional: Si es null, el backend usa la tarifa predeterminada
) {}