package com.educoders.config;

import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ejercita, al terminar de arrancar, lo que usa el inicio de sesión (consulta de usuario por correo y comparación
 * BCrypt), para que el primer usuario que entra tras un arranque no pague la inicialización en frío.
 * <p>
 * Solo lee: no crea sesiones ni escribe en la auditoría. Si falla, el arranque continúa y solo se deja un aviso.
 */
@Component
public class CalentamientoArranque {

    private static final Logger log = LoggerFactory.getLogger(CalentamientoArranque.class);

    /** Correo con dominio reservado (RFC 2606): no corresponde a ninguna cuenta. */
    private static final String CORREO_INEXISTENTE = "calentamiento@arranque.invalid";
    private static final String TEXTO_DE_PRUEBA = "calentamiento";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CalentamientoArranque(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void calentar() {
        try {
            usuarioRepository.findByEmail(CORREO_INEXISTENTE);
            passwordEncoder.matches(TEXTO_DE_PRUEBA, passwordEncoder.encode(TEXTO_DE_PRUEBA));
        } catch (RuntimeException ex) {
            log.warn("No se pudo completar el calentamiento de arranque: {}", ex.getMessage());
        }
    }
}
