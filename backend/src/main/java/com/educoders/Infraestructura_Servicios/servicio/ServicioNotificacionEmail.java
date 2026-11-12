package com.educoders.Infraestructura_Servicios.servicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ServicioNotificacionEmail {

    /**
     * Envía un correo informativo al apoderado cuando se emite una obligación de pago (Matrícula o Pensión).
     */
    void enviarCorreoCompromisoPago(
            String emailDestino,
            String nombreApoderado,
            String nombreEstudiante,
            String concepto,
            BigDecimal monto
    );

    void enviarCorreoValidacionPago(
            String emailDestino,
            String nombreApoderado,
            String nombreEstudiante,
            String concepto,
            BigDecimal monto
    );

    void enviarCorreoRechazoPago(
            String emailDestino,
            String nombreApoderado,
            String nombreEstudiante,
            String concepto,
            String motivo
    );

    /**
     * Envía el código de verificación para definir o restablecer la contraseña.
     */
    void enviarCorreoRestablecimientoPassword(
            String emailDestino,
            String nombreUsuario,
            String codigo,
            LocalDateTime fechaExpiracion
    );
}