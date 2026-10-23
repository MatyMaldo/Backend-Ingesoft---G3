
package com.educoders.Seguridad_Auditoria.seguridad;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.servicio.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UsuarioRepository usuarioRepository
    ) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        SecurityContextHolder.clearContext();

        String authorization = request.getHeader("Authorization");

        if (authorization != null
                && authorization.startsWith("Bearer ")) {

            String token = authorization.substring(7);

            try {
                Claims claims = jwtService.validarToken(token);

                String usuarioId = claims.getSubject();
                String rol = claims.get("rol", String.class);

                if (usuarioId != null && "ADMIN".equals(rol)) {

                    Long id = Long.valueOf(usuarioId);

                    Optional<Usuario> usuarioOptional =
                            usuarioRepository.findById(id);

                    if (usuarioOptional.isPresent()) {

                        Usuario usuario = usuarioOptional.get();

                        if (usuario.isActivo()
                                && usuario instanceof Administrador) {

                            var authorities = List.of(
                                    new SimpleGrantedAuthority("ROLE_ADMIN")
                            );

                            var authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            usuario.getId().toString(),
                                            null,
                                            authorities
                                    );

                            SecurityContextHolder.getContext()
                                    .setAuthentication(authentication);
                        }
                    }
                }

            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}