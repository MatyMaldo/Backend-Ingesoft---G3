package com.educoders.Infraestructura_Servicios.exception;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException de(String recurso, Object id) {
        return new RecursoNoEncontradoException(
                "No se encontró " + recurso + " con identificador " + id + ".");
    }
}
