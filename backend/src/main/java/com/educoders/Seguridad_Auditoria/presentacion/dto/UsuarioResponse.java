package com.educoders.Seguridad_Auditoria.presentacion.dto;

import java.time.LocalDateTime;

/**
 * Representación de un usuario del sistema.
 * No expone el hash de la contraseña.
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        String dni,
        String username,
        String email,
        String telefono,
        String tipoUsuario,
        boolean activo,
        LocalDateTime fechaCreacion
) {
}