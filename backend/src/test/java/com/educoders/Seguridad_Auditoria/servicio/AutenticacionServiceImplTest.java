
package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AutenticacionServiceImpl autenticacionService;

    private Docente docente;

    @BeforeEach
    void configurar() {
        docente = mock(Docente.class);
    }

    @Test
    void iniciarSesionConDniCorrecto() {
        when(usuarioRepository.findAllByDni("12345678"))
                .thenReturn(List.of(docente));

        when(docente.getPasswordHash()).thenReturn("hash");
        when(docente.isActivo()).thenReturn(true);
        when(docente.getId()).thenReturn(1L);
        when(docente.getNombre()).thenReturn("Carlos");

        when(passwordEncoder.matches("clave123", "hash"))
                .thenReturn(true);

        when(jwtService.generarToken(docente, "DOCENTE"))
                .thenReturn("token-prueba");

        LoginResponse respuesta = autenticacionService.iniciarSesion(
                new LoginRequest("12345678", "clave123")
        );

        assertEquals("token-prueba", respuesta.token());
        assertEquals("DOCENTE", respuesta.usuario().rol());

        verify(usuarioRepository).findAllByDni("12345678");
    }

    @Test
    void rechazarContrasenaIncorrecta() {
        when(usuarioRepository.findAllByDni("12345678"))
                .thenReturn(List.of(docente));

        when(docente.getPasswordHash()).thenReturn("hash");

        when(passwordEncoder.matches("incorrecta", "hash"))
                .thenReturn(false);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(
                        new LoginRequest("12345678", "incorrecta")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(jwtService);
    }

    @Test
    void rechazarCuentaSinContrasenaDefinida() {
        // Cuenta recién registrada: aún no definió su contraseña (HU-SAT-03), así que no puede iniciar sesión.
        when(usuarioRepository.findAllByDni("12345678"))
                .thenReturn(List.of(docente));

        when(docente.getPasswordHash()).thenReturn(null);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(
                        new LoginRequest("12345678", "cualquier-cosa")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void rechazarUsuarioInexistente() {
        when(usuarioRepository.findAllByDni("99999999"))
                .thenReturn(List.of());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(
                        new LoginRequest("99999999", "clave123")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(jwtService);
    }

    @Test
    void rechazarCuentaInactiva() {
        when(usuarioRepository.findAllByDni("12345678"))
                .thenReturn(List.of(docente));

        when(docente.getPasswordHash()).thenReturn("hash");
        when(docente.isActivo()).thenReturn(false);

        when(passwordEncoder.matches("clave123", "hash"))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(
                        new LoginRequest("12345678", "clave123")
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(jwtService);
    }

    @Test
    void iniciarSesionConCorreoCorrecto() {
        when(usuarioRepository.findByEmail("docente@educoders.com"))
                .thenReturn(Optional.of(docente));

        when(docente.getPasswordHash()).thenReturn("hash");
        when(docente.isActivo()).thenReturn(true);

        when(passwordEncoder.matches("clave123", "hash"))
                .thenReturn(true);

        when(jwtService.generarToken(docente, "DOCENTE"))
                .thenReturn("token-docente");

        LoginResponse respuesta = autenticacionService.iniciarSesion(
                new LoginRequest("docente@educoders.com", "clave123")
        );

        assertEquals("token-docente", respuesta.token());
        assertEquals("DOCENTE", respuesta.usuario().rol());

        verify(usuarioRepository).findByEmail("docente@educoders.com");
    }

    @Test
    void identificarAutomaticamenteLosCuatroRoles() {
        Usuario[] usuarios = {
                mock(Administrador.class),
                mock(Docente.class),
                mock(Estudiante.class),
                mock(Apoderado.class)
        };

        String[] roles = {
                "ADMINISTRADOR",
                "DOCENTE",
                "ESTUDIANTE",
                "APODERADO"
        };

        for (int i = 0; i < usuarios.length; i++) {
            Usuario usuario = usuarios[i];
            String rolEsperado = roles[i];
            String dni = String.format("%08d", i + 1);

            when(usuarioRepository.findAllByDni(dni))
                    .thenReturn(List.of(usuario));

            when(usuario.getPasswordHash()).thenReturn("hash");
            when(usuario.isActivo()).thenReturn(true);

            when(passwordEncoder.matches("clave123", "hash"))
                    .thenReturn(true);

            when(jwtService.generarToken(usuario, rolEsperado))
                    .thenReturn("token-" + rolEsperado);

            LoginResponse respuesta = autenticacionService.iniciarSesion(
                    new LoginRequest(dni, "clave123")
            );

            assertEquals(rolEsperado, respuesta.usuario().rol());
            assertEquals("token-" + rolEsperado, respuesta.token());
        }
    }

    @Test
    void identificarDocenteConDniCompartido() {
        Apoderado apoderado = mock(Apoderado.class);

        when(usuarioRepository.findAllByDni("12345678"))
                .thenReturn(List.of(docente, apoderado));

        when(docente.getPasswordHash()).thenReturn("hash-docente");
        when(apoderado.getPasswordHash()).thenReturn("hash-apoderado");

        when(passwordEncoder.matches("clave-docente", "hash-docente"))
                .thenReturn(true);

        when(passwordEncoder.matches("clave-docente", "hash-apoderado"))
                .thenReturn(false);

        when(docente.isActivo()).thenReturn(true);

        when(jwtService.generarToken(docente, "DOCENTE"))
                .thenReturn("token-docente");

        LoginResponse respuesta = autenticacionService.iniciarSesion(
                new LoginRequest("12345678", "clave-docente")
        );

        assertEquals("DOCENTE", respuesta.usuario().rol());
        assertEquals("token-docente", respuesta.token());
    }

    @Test
    void identificarApoderadoConDniCompartido() {
        Apoderado apoderado = mock(Apoderado.class);

        when(usuarioRepository.findAllByDni("12345678"))
                .thenReturn(List.of(docente, apoderado));

        when(docente.getPasswordHash()).thenReturn("hash-docente");
        when(apoderado.getPasswordHash()).thenReturn("hash-apoderado");

        when(passwordEncoder.matches("clave-apoderado", "hash-docente"))
                .thenReturn(false);

        when(passwordEncoder.matches("clave-apoderado", "hash-apoderado"))
                .thenReturn(true);

        when(apoderado.isActivo()).thenReturn(true);

        when(jwtService.generarToken(apoderado, "APODERADO"))
                .thenReturn("token-apoderado");

        LoginResponse respuesta = autenticacionService.iniciarSesion(
                new LoginRequest("12345678", "clave-apoderado")
        );

        assertEquals("APODERADO", respuesta.usuario().rol());
        assertEquals("token-apoderado", respuesta.token());
    }

    @Test
    void rechazarDniCompartidoConMismaContrasena() {
        Apoderado apoderado = mock(Apoderado.class);

        when(usuarioRepository.findAllByDni("12345678"))
                .thenReturn(List.of(docente, apoderado));

        when(docente.getPasswordHash()).thenReturn("hash-docente");
        when(apoderado.getPasswordHash()).thenReturn("hash-apoderado");

        when(passwordEncoder.matches("clave-compartida", "hash-docente"))
                .thenReturn(true);

        when(passwordEncoder.matches("clave-compartida", "hash-apoderado"))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(
                        new LoginRequest("12345678", "clave-compartida")
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(jwtService);
    }
}
