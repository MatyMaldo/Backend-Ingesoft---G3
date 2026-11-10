package com.educoders.Infraestructura_Servicios.operaciones;

/**
 * Se lanza cuando no hay capacidad en el pool para aceptar un nuevo job.
 */
public class OperacionMasivaRechazadaException extends RuntimeException {

    public OperacionMasivaRechazadaException(String mensaje) {
        super(mensaje);
    }
}
