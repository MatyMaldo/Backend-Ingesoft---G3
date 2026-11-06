package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import jakarta.validation.constraints.AssertTrue;

/**
 * Emisión de compromiso de pago de matrícula.
 *
 * Se puede identificar la matrícula de dos formas:
 * - Directamente con {@code matriculaId} (compatibilidad con el contrato anterior).
 * - Indirectamente con {@code estudianteId} + {@code anioId}; el backend resuelve
 *   la matrícula PENDIENTE del estudiante para ese año académico.
 *
 * El monto ya no se recibe desde el cliente: se toma de la configuración del backend.
 */
public record EmitirCompromisoMatriculaRequest(
        Long matriculaId,
        Long estudianteId,
        Long anioId
) {

    @AssertTrue(message = "Debe indicar matriculaId o, en su defecto, estudianteId junto con anioId.")
    public boolean isIdentificacionValida() {
        return matriculaId != null || (estudianteId != null && anioId != null);
    }
}
