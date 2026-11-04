package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * El validador se obtiene del token autenticado (no del cuerpo de la petición).
 */
public record RechazarPagoRequest(
        @NotBlank(message = "El motivo de rechazo es obligatorio")
        String motivo
) {}