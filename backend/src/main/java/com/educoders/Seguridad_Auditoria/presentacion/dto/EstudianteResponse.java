package com.educoders.Seguridad_Auditoria.presentacion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representación de un estudiante registrado.
 * Nunca expone el hash de la contraseña.
 */
public record EstudianteResponse(
        Long id,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        String dni,
        String username,
        String email,
        String telefono,
        String codigo,
        LocalDate fechaNacimiento,
        Long apoderadoId,
        boolean activo,
        LocalDateTime fechaCreacion
) {
}