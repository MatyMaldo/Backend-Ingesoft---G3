package com.educoders.Gestion_Matriz_Horarios.exception;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GuardadoSeccionErroresResponse;

/**
 * Se lanza cuando el guardado masivo de una sección detecta uno o más
 * problemas por bloque. Transporta el detalle para que la API responda 409 con
 * todos los conflictos.
 */
public class GuardadoBloquesConConflictosException extends RuntimeException {

    private final GuardadoSeccionErroresResponse errores;

    public GuardadoBloquesConConflictosException(GuardadoSeccionErroresResponse errores) {
        super("No se pudo guardar el horario: se encontraron "
                + errores.errores().size() + " conflicto(s).");
        this.errores = errores;
    }

    public GuardadoSeccionErroresResponse getErrores() {
        return errores;
    }
}
