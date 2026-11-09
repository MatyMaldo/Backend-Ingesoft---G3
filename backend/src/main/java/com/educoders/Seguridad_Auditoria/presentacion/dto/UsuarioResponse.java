package com.educoders.Seguridad_Auditoria.presentacion.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representación de un usuario del sistema.
 * No expone el hash de la contraseña.
 * {@code codigo} solo viene informado para estudiantes (es nulo en los demás tipos): permite al cliente
 * conocer los códigos ya asignados y no proponer uno repetido al registrar o importar estudiantes.
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
        LocalDateTime fechaCreacion,
        List<SeccionAfectadaInfo> seccionesAfectadas,
        String codigo
) {
    public record SeccionAfectadaInfo(
            Long seccionId,
            String seccionNombre,
            String gradoNombre,
            Integer anioAcademico
    ) {}

    public UsuarioResponse(
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
        this(id, nombre, apellidoPaterno, apellidoMaterno, nombreCompleto, dni, username, email, telefono, tipoUsuario, activo, fechaCreacion, null, null);
    }
}