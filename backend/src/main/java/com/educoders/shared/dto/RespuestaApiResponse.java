package com.educoders.shared.dto;

/**
 * Estructura uniforme utilizada por la API para respuestas exitosas y respuestas de error.
 *
 * @param datos contenido devuelto cuando la operación es exitosa
 * @param error mensaje de error cuando la operación no puede completarse
 * @param <T> tipo de datos devuelto por la operación
 */
public record RespuestaApiResponse<T>(T datos, String error) {

    public static <T> RespuestaApiResponse<T> crearExito(T datos) {
        return new RespuestaApiResponse<>(datos, null);
    }

    public static <T> RespuestaApiResponse<T> crearError(String error) {
        return new RespuestaApiResponse<>(null, error);
    }
}
