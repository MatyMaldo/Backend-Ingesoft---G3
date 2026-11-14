package com.educoders.Seguridad_Auditoria.presentacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos de entrada para HU-GEMAP-01.
 * El username se autogenera en el servicio a partir del DNI.
 * No recibe contraseña: la cuenta nace sin ella y no se envía ningún correo al registrarla. El usuario decide cuándo
 * ingresar y obtiene su contraseña con el restablecimiento (HU-SAT-03).
 * Recibe la fecha de nacimiento; la edad se calcula a partir de ella.
 */
public record RegistrarApoderadoRequest(

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 80, message = "El nombre no debe superar 80 caracteres.")
        String nombre,

        @NotBlank(message = "El apellido paterno es obligatorio.")
        @Size(max = 80, message = "El apellido paterno no debe superar 80 caracteres.")
        String apellidoPaterno,

        @Size(max = 80, message = "El apellido materno no debe superar 80 caracteres.")
        String apellidoMaterno,

        @NotBlank(message = "El DNI es obligatorio.")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos.")
        String dni,

        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 160, message = "El correo no debe superar 160 caracteres.")
        String email,

        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos.")
        String telefono,

        @NotNull(message = "La fecha de nacimiento es obligatoria.")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy.")
        LocalDate fechaNacimiento
) {
}