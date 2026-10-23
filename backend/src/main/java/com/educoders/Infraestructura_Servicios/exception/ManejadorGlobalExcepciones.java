package com.educoders.Infraestructura_Servicios.exception;

import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Centraliza la traducción de excepciones a respuestas HTTP uniformes.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    // 1. Inyección por constructor del nuevo servicio
    private final LogErrorService logErrorService;

    public ManejadorGlobalExcepciones(LogErrorService logErrorService) {
        this.logErrorService = logErrorService;
    }

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

    // 2. Método actualizado agregando HttpServletRequest para capturar URI y Método HTTP
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarErrorInesperado(
            Exception excepcion, HttpServletRequest request) {

        REGISTRO.error("Error inesperado en la aplicación", excepcion);

        // Identificar la primera clase de nuestro paquete que originó la excepción
        String claseOrigen = obtenerClaseOrigen(excepcion);

        logErrorService.registrarError(
                claseOrigen,
                "Excepción no controlada",
                excepcion.getMessage() != null ? excepcion.getMessage() : "Error sin mensaje explícito",
                convertirStackTraceATexto(excepcion),
                request.getRequestURI(),
                request.getMethod(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RespuestaApiResponse.crearError("Ocurrió un error inesperado"));
    }
    private String obtenerClaseOrigen(Throwable throwable) {
        if (throwable.getStackTrace() != null) {
            for (StackTraceElement elem : throwable.getStackTrace()) {
                if (elem.getClassName().startsWith("com.educoders")) {
                    return elem.getClassName();
                }
            }
        }
        return "Desconocido";
    }
    // Método auxiliar para convertir la pila de llamadas
    private String convertirStackTraceATexto(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarRegla(ReglaNegocioException ex) {
        REGISTRO.warn("Regla de negocio: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(RespuestaApiResponse.crearError(ex.getMessage()));
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarConflicto(ConflictoException ex) {
        REGISTRO.warn("Conflicto: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(RespuestaApiResponse.crearError(ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RespuestaApiResponse.crearError(ex.getMessage()));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarEstadoHttp(
            ResponseStatusException excepcion) {

        String mensaje = excepcion.getReason() != null
                ? excepcion.getReason()
                : "No se pudo completar la solicitud";

        return ResponseEntity
                .status(excepcion.getStatusCode())
                .body(RespuestaApiResponse.crearError(mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarJsonInvalido(
            HttpMessageNotReadableException excepcion) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(RespuestaApiResponse.crearError(
                        "El cuerpo de la solicitud no contiene un JSON válido"));
    }
}
