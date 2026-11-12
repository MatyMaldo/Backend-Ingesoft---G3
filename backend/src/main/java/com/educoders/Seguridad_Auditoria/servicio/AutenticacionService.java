package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginResponse;

public interface AutenticacionService {
    LoginResponse iniciarSesion(LoginRequest request);

    /** Registra en la auditoría el cierre de sesión voluntario del usuario. */
    void cerrarSesion(Long usuarioId);
}
