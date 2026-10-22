package com.educoders.Infraestructura_Servicios.servicio;

import java.math.BigDecimal;

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
}