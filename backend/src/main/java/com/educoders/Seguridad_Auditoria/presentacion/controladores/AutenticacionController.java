package com.educoders.Seguridad_Auditoria.presentacion.controladores;


import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.MensajeResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RestablecerPasswordRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.SolicitarCodigoRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.VerificacionCodigoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.VerificarCodigoRequest;
import com.educoders.Seguridad_Auditoria.servicio.AutenticacionService;
import com.educoders.Seguridad_Auditoria.servicio.RestablecimientoPasswordService;

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
    private final RestablecimientoPasswordService restablecimientoPasswordService;

    public AutenticacionController(
            AutenticacionService autenticacionService,
            RestablecimientoPasswordService restablecimientoPasswordService
    ) {
        this.autenticacionService = autenticacionService;
        this.restablecimientoPasswordService = restablecimientoPasswordService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> iniciarSesion(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response =
                autenticacionService.iniciarSesion(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/password/solicitar")
    public ResponseEntity<RespuestaApiResponse<MensajeResponse>> solicitarCodigo(
            @Valid @RequestBody SolicitarCodigoRequest request) {
        restablecimientoPasswordService.solicitarCodigo(request.email());
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(new MensajeResponse(
                "Si el correo está registrado, recibirás un código de verificación.")));
    }

    @PostMapping("/password/verificar")
    public ResponseEntity<RespuestaApiResponse<VerificacionCodigoResponse>> verificarCodigo(
            @Valid @RequestBody VerificarCodigoRequest request) {
        boolean valido = restablecimientoPasswordService.verificarCodigo(
                request.email(), request.codigo());
        String mensaje = valido
                ? "Código válido."
                : "El código es inválido o ha expirado.";
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                new VerificacionCodigoResponse(valido, mensaje)));
    }

    @PostMapping("/password/restablecer")
    public ResponseEntity<RespuestaApiResponse<MensajeResponse>> restablecerPassword(
            @Valid @RequestBody RestablecerPasswordRequest request) {
        restablecimientoPasswordService.restablecerPassword(
                request.email(), request.codigo(), request.nuevaPassword());
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                new MensajeResponse("Contraseña actualizada correctamente.")));
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
