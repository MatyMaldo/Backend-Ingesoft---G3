package com.educoders.Seguridad_Auditoria.presentacion.dto;
/**
 * Respuesta de un inicio de sesión exitoso.
 */
public record LoginResponse(
        String token,
        UsuarioLoginResponse usuario
) {
    /**
     * Información básica del usuario autenticado.
     */
    public record UsuarioLoginResponse(
            Long id,
            String nombre,
            String rol
    ) {
    }
}