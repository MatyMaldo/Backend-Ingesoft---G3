package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginRequest;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import com.educoders.Seguridad_Auditoria.servicio.AutenticacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Verificación manual contra la base de datos real (no corre en {@code mvn test}) de la atomicidad del inicio de
 * sesión (HU-SAT-07, Esc. 1 y 12): el inicio de sesión y su registro de auditoría se confirman juntos, y si la
 * auditoría no puede guardarse no se crea la sesión ni queda registro alguno.
 *
 * <p>IMPORTANTE: la base protege {@code auditorias} con triggers que prohíben UPDATE y DELETE, así que nada de lo que
 * se confirme se puede limpiar. Por eso cada prueba corre dentro de una transacción externa que SIEMPRE se revierte
 * (el servicio de autenticación se une a ella) y al final se comprueba que no quedó ninguna fila. También se revierte
 * la contraseña de prueba que se fija a la cuenta usada. Se activa con {@code -Dtest.bd=true}.
 *
 * <p>No pasa por HTTP a propósito: el manejador global escribe el log de errores en una transacción propia
 * ({@code REQUIRES_NEW}) que se confirma y no se puede borrar. Ese tramo lo cubre {@code LoginLogErroresHttpTest}.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "test.bd", matches = "true")
class LoginAtomicidadBdTest {

    private static final String CLAVE = "ClaveDePrueba123";
    private static final String SESION_INICIADA = "Inicio de sesión";

    @Autowired AutenticacionService autenticacionService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    /** Se envuelve para poder hacer fallar la auditoría sin tocar el servicio real cuando no se configura. */
    @SpyBean AuditoriaService auditoriaService;

    /** Cuenta activa existente cuyo correo se usa en la prueba: la contraseña se fija dentro de la transacción. */
    private Usuario cuentaActiva() {
        Long id = jdbc.queryForObject("SELECT MIN(id) FROM usuarios WHERE activo = TRUE", Long.class);
        assertNotNull(id, "la BD local necesita al menos una cuenta activa");
        return usuarioRepository.findById(id).orElseThrow();
    }

    private long iniciosDeSesionDe(Long usuarioId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM auditorias WHERE entidad = 'Sesion' AND entidad_id = ? AND descripcion = ?",
                Long.class, usuarioId, SESION_INICIADA);
    }

    @Test
    void elInicioDeSesionYSuAuditoriaSeConfirmanJuntos() {
        Usuario cuenta = cuentaActiva();
        Long id = cuenta.getId();
        String correo = cuenta.getEmail();
        long antes = iniciosDeSesionDe(id);
        long[] dentro = new long[1];

        new TransactionTemplate(txManager).executeWithoutResult(estado -> {
            cuentaActiva().setPasswordHash(passwordEncoder.encode(CLAVE));
            usuarioRepository.flush();

            var respuesta = autenticacionService.iniciarSesion(new LoginRequest(correo, CLAVE));

            assertNotNull(respuesta.token(), "el inicio de sesión correcto entrega el token");
            dentro[0] = iniciosDeSesionDe(id);
            // No se confirma: los triggers impiden borrar después.
            estado.setRollbackOnly();
        });

        assertEquals(antes + 1, dentro[0], "la auditoría debe verse dentro de la misma transacción del inicio de sesión");
        assertEquals(antes, iniciosDeSesionDe(id), "al revertir no debe quedar el registro de auditoría");
    }

    @Test
    void siLaAuditoriaFallaNoSeCreaLaSesionNiQuedaRegistro() {
        Usuario cuenta = cuentaActiva();
        Long id = cuenta.getId();
        String correo = cuenta.getEmail();
        long antes = iniciosDeSesionDe(id);
        long[] dentro = new long[1];
        new TransactionTemplate(txManager).executeWithoutResult(estado -> {
            // El método es MANDATORY: hasta el stubbing necesita la transacción abierta.
            doThrow(new IllegalStateException("fallo simulado de la auditoría")).when(auditoriaService).registrarAuditoria(any());
            cuentaActiva().setPasswordHash(passwordEncoder.encode(CLAVE));
            usuarioRepository.flush();

            // Sin token: el error se propaga y el usuario no obtiene sesión (Esc. 12).
            assertThrows(IllegalStateException.class,
                    () -> autenticacionService.iniciarSesion(new LoginRequest(correo, CLAVE)));

            dentro[0] = iniciosDeSesionDe(id);
            estado.setRollbackOnly();
        });

        assertEquals(antes, dentro[0], "no debe haber registro de inicio de sesión si la auditoría falló");
        assertEquals(antes, iniciosDeSesionDe(id));
    }
}
