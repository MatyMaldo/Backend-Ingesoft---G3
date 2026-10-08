package com.educoders.Infraestructura_Servicios.exception;

import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centraliza la traducción de excepciones a respuestas HTTP uniformes.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarValidacion(MethodArgumentNotValidException excepcion) {
        String mensaje = excepcion.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(errorCampo -> errorCampo.getDefaultMessage())
                .orElse("Los datos enviados no son válidos");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(RespuestaApiResponse.crearError(mensaje));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarErrorInesperado(Exception excepcion) {
        REGISTRO.error("Error inesperado en la aplicación", excepcion);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RespuestaApiResponse.crearError("Ocurrió un error inesperado"));
    }
}
