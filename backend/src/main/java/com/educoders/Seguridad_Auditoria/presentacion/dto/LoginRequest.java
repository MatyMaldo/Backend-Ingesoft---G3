package com.educoders.Seguridad_Auditoria.presentacion.dto;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(

        @NotBlank(message = "Debe seleccionar un perfil.")
        String perfil,

        @NotBlank(message = "El correo electrónico o DNI es obligatorio.")
        @Pattern(
                regexp = "^(?:[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}|[0-9]{8})$",
                message = "Ingrese un correo electrónico válido o un DNI de 8 dígitos."
        )
        String identificador,

        @NotBlank(message = "La contraseña es obligatoria.")
        String password

) {
}