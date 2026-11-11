package com.educoders.Seguridad_Auditoria.presentacion.dto;

/**
 * Desenlace de una fila de la carga masiva de usuarios.
 *
 * @param linea  número de línea del archivo
 * @param estado {@code CREADO} o {@code RECHAZADO}
 * @param motivo por qué se rechazó; nulo si se creó
 * @param codigo código del estudiante creado; nulo en los demás casos
 */
public record ResultadoFilaUsuario(int linea, String estado, String motivo, String codigo) {

    public static final String CREADO = "CREADO";
    public static final String RECHAZADO = "RECHAZADO";

    public static ResultadoFilaUsuario creado(int linea, String codigo) {
        return new ResultadoFilaUsuario(linea, CREADO, null, codigo);
    }

    public static ResultadoFilaUsuario rechazado(int linea, String motivo) {
        return new ResultadoFilaUsuario(linea, RECHAZADO, motivo, null);
    }
}
