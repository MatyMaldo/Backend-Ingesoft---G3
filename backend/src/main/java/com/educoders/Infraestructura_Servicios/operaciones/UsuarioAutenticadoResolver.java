package com.educoders.Infraestructura_Servicios.operaciones;

import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Obtiene el identificador del usuario autenticado en el hilo HTTP.
 * <p>
 * Se usa únicamente al crear el job. El worker nunca consulta el
 * {@code SecurityContext}; recibe el ID de forma explícita.
 */
@Component
public class UsuarioAutenticadoResolver {

    public Long idActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return usuario.getId();
    }
}
