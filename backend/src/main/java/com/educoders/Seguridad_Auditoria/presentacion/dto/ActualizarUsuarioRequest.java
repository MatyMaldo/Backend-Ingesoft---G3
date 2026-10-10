package com.educoders.Seguridad_Auditoria.presentacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/* Datos editables de un usuario (HU-SAT-02). Actualización PARCIAL:
        * solo se modifican los campos enviados (no nulos). Los campos no
 enviados quedan intactos. (Depende de como lo hagan en el front xd)*/

public record ActualizarUsuarioRequest(
        @Size(max = 80, message = "El nombre no debe superar 80 caracteres.")
        String nombre,

        @Size(max = 80, message = "El apellido paterno no debe superar 80 caracteres.")
        String apellidoPaterno,

        @Size(max = 80, message = "El apellido materno no debe superar 80 caracteres.")
        String apellidoMaterno,

        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos.")
        String dni,

        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 160, message = "El correo no debe superar 160 caracteres.")
        String email,

        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos.")
        String telefono
) { }
