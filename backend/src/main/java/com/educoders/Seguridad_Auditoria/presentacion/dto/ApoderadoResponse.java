package com.educoders.Seguridad_Auditoria.presentacion.dto;

import java.time.LocalDateTime;

/**
 * Representación de un apoderado recién registrado.
 * Nunca expone el hash de la contraseña.
 */
public record ApoderadoResponse(
        Long id,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        String dni,
        String username,
        String email,
        String telefono,
        Integer edad,
        boolean activo,
        LocalDateTime fechaCreacion
) {
}