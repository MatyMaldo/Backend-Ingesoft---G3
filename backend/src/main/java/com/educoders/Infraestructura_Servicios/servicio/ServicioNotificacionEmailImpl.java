package com.educoders.Infraestructura_Servicios.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
public class ServicioNotificacionEmailImpl implements ServicioNotificacionEmail {

    private static final Logger log = LoggerFactory.getLogger(ServicioNotificacionEmailImpl.class);

    private final JavaMailSender mailSender;
    private final Executor ejecutorCorreos;

    @Value("${MAIL_FROM}")
    private String remitente;

    public ServicioNotificacionEmailImpl(JavaMailSender mailSender,
                                         @Qualifier("correosExecutor") Executor ejecutorCorreos) {
        this.mailSender = mailSender;
        this.ejecutorCorreos = ejecutorCorreos;
    }

    /**
     * Envía el correo sin hacer esperar a quien lo pidió. El SMTP tarda segundos; si se enviara dentro de la
     * petición, emitir un compromiso o validar un pago esperaría a que el servidor de correo responda (y, sin
     * tiempo límite, podría quedar colgado). Si hay una transacción en curso, el envío se programa para después
     * del commit: no se avisa de algo que luego se revierte. Un fallo del envío solo se registra: la operación
     * de negocio ya terminó.
     */
    private void enviarSinEsperar(SimpleMailMessage mensaje, String descripcion, String emailDestino) {
        Runnable envio = () -> {
            try {
                mailSender.send(mensaje);
                log.info("Correo de {} enviado a: {}", descripcion, emailDestino);
            } catch (Exception e) {
                log.error("No se pudo enviar correo de {} a {}: {}", descripcion, emailDestino, e.getMessage());
            }
        };
        Runnable programar = () -> {
            try {
                ejecutorCorreos.execute(envio);
            } catch (RejectedExecutionException e) {
                log.error("Cola de correos llena: no se envió el correo de {} a {}", descripcion, emailDestino);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    programar.run();
                }
            });
        } else {
            programar.run();
        }
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
            mensaje.setFrom(remitente);
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

            enviarSinEsperar(mensaje, "compromiso de pago", emailDestino);

        } catch (Exception e) {
            // Se registra el error en consola pero NO se interrumpe la transacción del negocio
            log.error("No se pudo preparar el correo a {}: {}", emailDestino, e.getMessage());
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
            mensaje.setFrom(remitente);
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

            enviarSinEsperar(mensaje, "validación de pago", emailDestino);
        } catch (Exception e) {
            log.error("No se pudo preparar el correo de validación a {}: {}", emailDestino, e.getMessage());
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
            mensaje.setFrom(remitente);
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

            enviarSinEsperar(mensaje, "rechazo de pago", emailDestino);
        } catch (Exception e) {
            log.error("No se pudo preparar el correo de rechazo a {}: {}", emailDestino, e.getMessage());
        }
    }

    @Override
    public void enviarCorreoRestablecimientoPassword(
            String emailDestino,
            String nombreUsuario,
            String codigo,
            LocalDateTime fechaExpiracion) {

        if (emailDestino == null || emailDestino.isBlank()) {
            log.warn("No se pudo enviar correo de restablecimiento: correo vacío.");
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(emailDestino);
            mensaje.setSubject("Educoders - Código de restablecimiento de contraseña");
            mensaje.setText(String.format(
                    "Hola %s,\n\n" +
                            "Recibimos una solicitud para definir o restablecer tu contraseña.\n\n" +
                            "Código de verificación: %s\n" +
                            "Vence: %s\n\n" +
                            "Si no solicitaste este cambio, puedes ignorar este mensaje.\n\n" +
                            "Atentamente,\n" +
                            "Administración EduCoders",
                    nombreUsuario,
                    codigo,
                    fechaExpiracion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            ));

            mailSender.send(mensaje);
            log.info("Correo de restablecimiento enviado a: {}", emailDestino);
        } catch (Exception e) {
            // El envío no debe tumbar la solicitud; el usuario puede reintentar.
            log.error("No se pudo enviar correo de restablecimiento a {}: {}",
                    emailDestino, e.getMessage());
        }
    }
}