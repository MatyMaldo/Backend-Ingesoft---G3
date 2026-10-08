package com.educoders.Infraestructura_Servicios.exception;

/** Conflicto de unicidad o estado inválido (HTTP 409). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}