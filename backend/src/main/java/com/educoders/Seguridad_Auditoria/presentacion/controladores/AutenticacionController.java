package com.educoders.Seguridad_Auditoria.presentacion.controladores;


import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginResponse;
import com.educoders.Seguridad_Auditoria.servicio.AutenticacionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
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
}
