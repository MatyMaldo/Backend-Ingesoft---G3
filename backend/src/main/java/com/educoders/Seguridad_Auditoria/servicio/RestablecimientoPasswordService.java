package com.educoders.Seguridad_Auditoria.servicio;

public interface RestablecimientoPasswordService {

    /**
     * Genera y envía un código de verificación si el correo corresponde a un
     * usuario activo. La respuesta al cliente siempre es genérica.
     */
    void solicitarCodigo(String email);

    /**
     * Valida el código sin consumirlo. Devuelve {@code false} para cualquier
     * código inválido, expirado o bloqueado.
     */
    boolean verificarCodigo(String email, String codigo);

    /**
     * Valida nuevamente el código, actualiza la contraseña y consume el código.
     */
    void restablecerPassword(String email, String codigo, String nuevaPassword);
}
