package com.educoders.Infraestructura_Servicios.exception;

import com.educoders.Gestion_Matriz_Horarios.exception.GuardadoBloquesConConflictosException;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GuardadoSeccionErroresResponse;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Infraestructura_Servicios.enums.TipoError;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasivaRechazadaException;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
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

    /**
     * Parámetro obligatorio ausente: es un error de quien llama, no una excepción no controlada, así que responde
     * 400 y NO se registra en el log de errores (HU-SAT-06 solo admite excepciones no controladas, procesos
     * automáticos y notificaciones por correo).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarParametroFaltante(
            MissingServletRequestParameterException excepcion) {
        return ResponseEntity.badRequest().body(RespuestaApiResponse.crearError(
                "Falta el parámetro obligatorio '" + excepcion.getParameterName() + "'."));
    }

    /** Parámetro con un valor del tipo equivocado (por ejemplo, una fecha mal escrita): 400, sin registrar en el log. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarParametroInvalido(
            MethodArgumentTypeMismatchException excepcion) {
        return ResponseEntity.badRequest().body(RespuestaApiResponse.crearError(
                "El valor del parámetro '" + excepcion.getName() + "' no es válido."));
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
                TipoError.EXCEPCION_NO_CONTROLADA,
                excepcion.getMessage() != null ? excepcion.getMessage() : "Error sin mensaje explícito",
                convertirStackTraceATexto(excepcion),
                request.getRequestURI(),
                request.getMethod(),
                (short) HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RespuestaApiResponse.crearError("Se registró un error inesperado"));
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

    @ExceptionHandler(OperacionMasivaRechazadaException.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarOperacionRechazada(
            OperacionMasivaRechazadaException ex) {
        REGISTRO.warn("Operación masiva rechazada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(RespuestaApiResponse.crearError(ex.getMessage()));
    }

    @ExceptionHandler(GuardadoBloquesConConflictosException.class)
    public ResponseEntity<RespuestaApiResponse<GuardadoSeccionErroresResponse>>
    manejarGuardadoConConflictos(GuardadoBloquesConConflictosException ex) {
        GuardadoSeccionErroresResponse detalle = ex.getErrores();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RespuestaApiResponse<>(
                        detalle,
                        "No se guardó el horario: " + detalle.errores().size()
                                + " conflicto(s)."));
    }

    /**
     * Endpoint inexistente: responde 404 y no 500. NO se registra en el log de errores: es una solicitud a una ruta
     * que no existe, no una excepción no controlada (HU-SAT-06 solo admite excepciones no controladas, procesos
     * automáticos y notificaciones por correo).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespuestaApiResponse<Void>> manejarRutaNoEncontrada(NoResourceFoundException ex) {
        REGISTRO.warn("Ruta inexistente: {}", ex.getResourcePath());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RespuestaApiResponse.crearError(
                        "Recurso no encontrado: " + ex.getResourcePath()));
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

        // Verificar si es un error de deserialización de enum
        Throwable causa = excepcion.getCause();
        if (causa instanceof com.fasterxml.jackson.databind.JsonMappingException) {
            com.fasterxml.jackson.databind.JsonMappingException jsonEx = (com.fasterxml.jackson.databind.JsonMappingException) causa;
            if (jsonEx.getPath() != null && !jsonEx.getPath().isEmpty()) {
                String fieldName = jsonEx.getPath().get(jsonEx.getPath().size() - 1).getFieldName();
                
                // Personalizar mensaje para el campo valorNota
                if ("valorNota".equals(fieldName)) {
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(RespuestaApiResponse.crearError(
                                    "La nota debe pertenecer a la escala del CNEB (AD, A, B, C)"));
                }
            }
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(RespuestaApiResponse.crearError(
                        "El cuerpo de la solicitud no contiene un JSON válido"));
    }
}
