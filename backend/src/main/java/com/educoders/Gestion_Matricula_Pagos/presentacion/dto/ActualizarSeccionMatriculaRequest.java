package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para cambiar únicamente la sección de una matrícula PENDIENTE.
 */
public record ActualizarSeccionMatriculaRequest(
        @NotNull(message = "La sección es obligatoria.")
        Long seccionId
) {
}
