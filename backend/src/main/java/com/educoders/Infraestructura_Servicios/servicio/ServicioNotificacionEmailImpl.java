package com.educoders.Infraestructura_Servicios.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ServicioNotificacionEmailImpl implements ServicioNotificacionEmail {

    private static final Logger log = LoggerFactory.getLogger(ServicioNotificacionEmailImpl.class);

    private final JavaMailSender mailSender;

    public ServicioNotificacionEmailImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarCorreoCompromisoPago(
            String emailDestino,
            String nombreApoderado,
            String nombreEstudiante,
            String concepto,
            BigDecimal monto) {

        if (emailDestino == null || emailDestino.isBlank()) {
            log.warn("No se pudo enviar correo: El apoderado no tiene un email registrado.");
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(emailDestino);
            mensaje.setSubject("Educoders - Emisión de Compromiso de Pago: " + concepto);
            mensaje.setText(String.format(
                    "Estimado(a) %s,\n\n" +
                            "Le informamos que se ha generado un nuevo compromiso de pago para el estudiante %s.\n\n" +
                            "Concepto: %s\n" +
                            "Monto Total: S/ %.2f\n\n" +
                            "Puede ingresar a la plataforma para adjuntar el comprobante de pago correspondiente.\n\n" +
                            "Atentamente,\n" +
                            "Administración EduCoders",
                    nombreApoderado, nombreEstudiante, concepto, monto
            ));

            mailSender.send(mensaje);
            log.info("Correo de notificación enviado exitosamente a: {}", emailDestino);

        } catch (Exception e) {
            // Se registra el error en consola pero NO se interrumpe la transacción del negocio
            log.error("No se pudo enviar el correo a {}: {}", emailDestino, e.getMessage());
        }
    }

    @Override
    public void enviarCorreoValidacionPago(
            String emailDestino,
            String nombreApoderado,
            String nombreEstudiante,
            String concepto,
            BigDecimal monto) {

        if (emailDestino == null || emailDestino.isBlank()) {
            log.warn("No se pudo enviar correo: El apoderado no tiene un email registrado.");
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(emailDestino);
            mensaje.setSubject("Educoders - Confirmación de Pago Validado: " + concepto);
            mensaje.setText(String.format(
                    "Estimado(a) %s,\n\n" +
                            "Le informamos que el pago registrado para el estudiante %s ha sido VALIDADO exitosamente.\n\n" +
                            "Concepto: %s\n" +
                            "Monto Validado: S/ %.2f\n\n" +
                            "Atentamente,\n" +
                            "Administración EduCoders",
                    nombreApoderado, nombreEstudiante, concepto, monto
            ));

            mailSender.send(mensaje);
            log.info("Correo de validación enviado a: {}", emailDestino);
        } catch (Exception e) {
            log.error("No se pudo enviar correo de validación a {}: {}", emailDestino, e.getMessage());
        }
    }

    @Override
    public void enviarCorreoRechazoPago(
            String emailDestino,
            String nombreApoderado,
            String nombreEstudiante,
            String concepto,
            String motivo) {

        if (emailDestino == null || emailDestino.isBlank()) {
            log.warn("No se pudo enviar correo: El apoderado no tiene un email registrado.");
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(emailDestino);
            mensaje.setSubject("Educoders - Rechazo de Comprobante de Pago: " + concepto);
            mensaje.setText(String.format(
                    "Estimado(a) %s,\n\n" +
                            "Le informamos que el pago registrado para el estudiante %s ha sido RECHAZADO.\n\n" +
                            "Concepto: %s\n" +
                            "Motivo de rechazo: %s\n\n" +
                            "Por favor, ingrese a la plataforma para regularizar la situación y adjuntar un nuevo comprobante valido.\n\n" +
                            "Atentamente,\n" +
                            "Administración EduCoders",
                    nombreApoderado, nombreEstudiante, concepto, motivo
            ));

            mailSender.send(mensaje);
            log.info("Correo de rechazo enviado a: {}", emailDestino);
        } catch (Exception e) {
            log.error("No se pudo enviar correo de rechazo a {}: {}", emailDestino, e.getMessage());
        }
    }
}