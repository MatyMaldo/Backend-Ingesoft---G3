package com.educoders.Seguridad_Auditoria.presentacion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representación de un apoderado recién registrado.
 * Nunca expone el hash de la contraseña. La edad se calcula a partir de la fecha de nacimiento.
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
        LocalDate fechaNacimiento,
        Integer edad,
        boolean activo,
        LocalDateTime fechaCreacion
) {
}