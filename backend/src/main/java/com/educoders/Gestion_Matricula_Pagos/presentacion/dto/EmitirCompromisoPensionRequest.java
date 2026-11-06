package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.Mes;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Emisión de compromiso de pago de pensión.
 *
 * Se puede identificar la matrícula de dos formas:
 * - Directamente con {@code matriculaId} (compatibilidad con el contrato anterior).
 * - Indirectamente con {@code estudianteId} + {@code anioId}; el backend resuelve
 *   la matrícula CONFIRMADA del estudiante para ese año académico.
 *
 * El monto ya no se recibe desde el cliente: se toma de la configuración del backend.
 */
public record EmitirCompromisoPensionRequest(
        Long matriculaId,
        Long estudianteId,
        Long anioId,
        @NotNull(message = "El mes correspondiente a la pensión es obligatorio.")
        Mes mes
) {

    @AssertTrue(message = "Debe indicar matriculaId o, en su defecto, estudianteId junto con anioId.")
    public boolean isIdentificacionValida() {
        return matriculaId != null || (estudianteId != null && anioId != null);
    }
}
