
package com.educoders.Seguridad_Auditoria.seguridad;

import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.servicio.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void configurar() {
        SecurityContextHolder.clearContext();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void configurarToken(String token, String rol) {
        Claims claims = mock(Claims.class);

        when(jwtService.validarToken(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("rol", String.class)).thenReturn(rol);

        request.addHeader("Authorization", "Bearer " + token);
    }

    @Test
    void autenticarDocenteConTokenValido() throws Exception {
        configurarToken("token-valido", "DOCENTE");

        Docente docente = mock(Docente.class);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(docente));

        when(docente.isActivo()).thenReturn(true);

        jwtFilter.doFilter(request, response, filterChain);

        var authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertEquals(docente, authentication.getPrincipal());

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCENTE")));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void rechazarTokenConRolIncorrecto() throws Exception {
        configurarToken("token-incorrecto", "ADMINISTRADOR");

        Docente docente = mock(Docente.class);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(docente));

        when(docente.isActivo()).thenReturn(true);

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void rechazarUsuarioInactivo() throws Exception {
        configurarToken("token-inactivo", "DOCENTE");

        Docente docente = mock(Docente.class);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(docente));

        when(docente.isActivo()).thenReturn(false);

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void rechazarTokenInvalido() throws Exception {
        request.addHeader("Authorization", "Bearer token-invalido");

        when(jwtService.validarToken("token-invalido"))
                .thenThrow(new JwtException("Token inválido"));

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verifyNoInteractions(usuarioRepository);
        verify(filterChain).doFilter(request, response);
    }
}
