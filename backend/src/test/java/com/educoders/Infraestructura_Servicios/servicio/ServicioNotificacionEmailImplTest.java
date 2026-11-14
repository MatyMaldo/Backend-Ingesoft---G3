package com.educoders.Infraestructura_Servicios.servicio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * El envío de correos de pagos no debe hacer esperar la petición: se programa en otro hilo y, dentro de una
 * transacción, solo después del commit.
 */
class ServicioNotificacionEmailImplTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final List<Runnable> tareas = new ArrayList<>();
    private final ServicioNotificacionEmailImpl servicio =
            new ServicioNotificacionEmailImpl(mailSender, tareas::add);

    ServicioNotificacionEmailImplTest() {
        ReflectionTestUtils.setField(servicio, "remitente", "colegio@educoders.test");
    }

    @AfterEach
    void limpiarTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void enviarCompromiso() {
        servicio.enviarCorreoCompromisoPago(
                "apoderado@correo.com", "Ana Perez", "Luis Perez", "Pensión Mensual - MARZO", new BigDecimal("450.00"));
    }

    @Test
    void elEnvioSeEntregaAlEjecutorSinEnviarEnElHiloDeLaPeticion() {
        enviarCompromiso();

        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        assertEquals(1, tareas.size());

        tareas.get(0).run();
        verify(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void dentroDeUnaTransaccionSeProgramaSoloDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();

        enviarCompromiso();

        assertTrue(tareas.isEmpty(), "No debe programarse el envío antes del commit");

        for (TransactionSynchronization sincronizacion : TransactionSynchronizationManager.getSynchronizations()) {
            sincronizacion.afterCommit();
        }
        assertEquals(1, tareas.size());
    }

    @Test
    void siLaTransaccionSeRevierteNoSeEnviaNada() {
        TransactionSynchronizationManager.initSynchronization();

        enviarCompromiso();

        for (TransactionSynchronization sincronizacion : TransactionSynchronizationManager.getSynchronizations()) {
            sincronizacion.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        assertTrue(tareas.isEmpty());
    }

    @Test
    void unFalloDelSmtpNoSePropagaNiRompeLaOperacion() {
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP caído"))
                .when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        enviarCompromiso();
        tareas.get(0).run(); // no debe lanzar
    }

    @Test
    void conLaColaLlenaLaOperacionContinua() {
        ServicioNotificacionEmailImpl lleno = new ServicioNotificacionEmailImpl(mailSender, tarea -> {
            throw new RejectedExecutionException("cola llena");
        });
        ReflectionTestUtils.setField(lleno, "remitente", "colegio@educoders.test");

        lleno.enviarCorreoValidacionPago(
                "apoderado@correo.com", "Ana Perez", "Luis Perez", "Pensión", new BigDecimal("450.00")); // no lanza
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void sinCorreoDeDestinoNoProgramaNada() {
        servicio.enviarCorreoCompromisoPago(" ", "Ana", "Luis", "Pensión", BigDecimal.TEN);

        assertTrue(tareas.isEmpty());
    }
}
