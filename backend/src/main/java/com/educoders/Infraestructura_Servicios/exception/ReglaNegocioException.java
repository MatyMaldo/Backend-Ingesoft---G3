package com.educoders.Infraestructura_Servicios.exception;

/** Regla de negocio incumplida (HTTP 400). */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}