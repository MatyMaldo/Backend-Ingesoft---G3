package com.educoders.Seguridad_Auditoria.presentacion.controladores;


import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginResponse;
import com.educoders.Seguridad_Auditoria.servicio.AutenticacionService;

import jakarta.validation.Valid;

import com.educoders.Seguridad_Auditoria.dominio.Usuario;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(
            AutenticacionService autenticacionService
    ) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> iniciarSesion(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response =
                autenticacionService.iniciarSesion(request);

        return ResponseEntity.ok(response);
    }

    /** Cierre de sesión voluntario (HU-SAT-07, Esc. 7): requiere sesión y queda en la auditoría. */
    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(Authentication autenticacion) {
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        autenticacionService.cerrarSesion(usuario.getId());
        return ResponseEntity.noContent().build();
    }
}
