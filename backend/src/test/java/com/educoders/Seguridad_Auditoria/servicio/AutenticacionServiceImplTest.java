
package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

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

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private AutenticacionServiceImpl autenticacionService;

    private Docente docente;

    @BeforeEach
    void configurar() {
        docente = mock(Docente.class);
    }

    @Test
    void rechazarContrasenaIncorrecta() {
        when(usuarioRepository.findByEmail("docente@educoders.com")).thenReturn(Optional.of(docente));
        when(docente.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(new LoginRequest("docente@educoders.com", "incorrecta")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals("Correo o contraseña incorrectos", error.getReason());
        verifyNoInteractions(jwtService);
    }

    @Test
    void rechazarCuentaSinContrasenaDefinida() {
        // Cuenta recién registrada: aún no definió su contraseña (HU-SAT-03), así que no puede iniciar sesión.
        when(usuarioRepository.findByEmail("docente@educoders.com")).thenReturn(Optional.of(docente));
        when(docente.getPasswordHash()).thenReturn(null);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(new LoginRequest("docente@educoders.com", "cualquier-cosa")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void rechazarUsuarioInexistente() {
        when(usuarioRepository.findByEmail("nadie@educoders.com")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(new LoginRequest("nadie@educoders.com", "clave123")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(jwtService);
    }

    @Test
    void rechazarCuentaInactiva() {
        when(usuarioRepository.findByEmail("docente@educoders.com")).thenReturn(Optional.of(docente));
        when(docente.getPasswordHash()).thenReturn("hash");
        when(docente.isActivo()).thenReturn(false);
        when(passwordEncoder.matches("clave123", "hash")).thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(new LoginRequest("docente@educoders.com", "clave123")));

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

    // ───────────── Auditoría de sesión (HU-SAT-04 / HU-SAT-07) ─────────────

    private RegistrarAuditoriaRequestDTO auditoriaRegistrada() {
        ArgumentCaptor<RegistrarAuditoriaRequestDTO> captor = ArgumentCaptor.forClass(RegistrarAuditoriaRequestDTO.class);
        verify(auditoriaService).registrarAuditoria(captor.capture());
        return captor.getValue();
    }

    private void assertEventoDeSesion(RegistrarAuditoriaRequestDTO auditoria, String descripcion, Long autorEsperado,
                                      String sesionAntes, String sesionDespues) {
        assertEquals(CategoriaAuditoria.USUARIO, auditoria.getCategoria());
        assertEquals(TipoAccionAuditoria.LOGIN, auditoria.getAccion());
        assertEquals(descripcion, auditoria.getDescripcion());
        assertEquals(autorEsperado, auditoria.getUsuarioId());
        assertTrue(auditoria.isAutorFijo(), "el autor del evento de sesión no debe sustituirse por el del token");
        assertEquals(1, auditoria.getDetalles().size());
        assertEquals(sesionAntes, auditoria.getDetalles().get(0).getValorAnterior());
        assertEquals(sesionDespues, auditoria.getDetalles().get(0).getValorNuevo());
    }

    @Test
    void inicioDeSesionExitosoQuedaEnLaAuditoria() {
        when(usuarioRepository.findByEmail("docente@educoders.com")).thenReturn(Optional.of(docente));
        when(docente.getPasswordHash()).thenReturn("hash");
        when(docente.isActivo()).thenReturn(true);
        when(docente.getId()).thenReturn(7L);
        when(passwordEncoder.matches("clave123", "hash")).thenReturn(true);
        when(jwtService.generarToken(docente, "DOCENTE")).thenReturn("token");

        autenticacionService.iniciarSesion(new LoginRequest("docente@educoders.com", "clave123"));

        assertEventoDeSesion(auditoriaRegistrada(), "Inicio de sesión", 7L, "Sin sesión", "Sesión iniciada");
    }

    @Test
    void siLaAuditoriaDelInicioDeSesionFallaNoSeCreaLaSesion() {
        when(usuarioRepository.findByEmail("docente@educoders.com")).thenReturn(Optional.of(docente));
        when(docente.getPasswordHash()).thenReturn("hash");
        when(docente.isActivo()).thenReturn(true);
        when(passwordEncoder.matches("clave123", "hash")).thenReturn(true);
        when(jwtService.generarToken(docente, "DOCENTE")).thenReturn("token");
        doThrow(new IllegalStateException("BD caída")).when(auditoriaService).registrarAuditoria(any());

        assertThrows(IllegalStateException.class, () -> autenticacionService.iniciarSesion(
                new LoginRequest("docente@educoders.com", "clave123")));
    }

    @Test
    void correoNoRegistradoGeneraIntentoFallidoConAutorNoDeterminado() {
        when(usuarioRepository.findByEmail("nadie@educoders.com")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(new LoginRequest("nadie@educoders.com", "clave123")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEventoDeSesion(auditoriaRegistrada(), "Intento fallido de inicio de sesión", null,
                "Sin sesión", "Sin sesión");
    }

    @Test
    void contrasenaIncorrectaGeneraIntentoFallidoDelTitular() {
        when(usuarioRepository.findByEmail("docente@educoders.com")).thenReturn(Optional.of(docente));
        when(docente.getPasswordHash()).thenReturn("hash");
        when(docente.getId()).thenReturn(7L);
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(new LoginRequest("docente@educoders.com", "mala")));

        assertEventoDeSesion(auditoriaRegistrada(), "Intento fallido de inicio de sesión", 7L,
                "Sin sesión", "Sin sesión");
    }

    @Test
    void cuentaSinContrasenaGeneraIntentoFallidoDelTitular() {
        when(usuarioRepository.findByEmail("docente@educoders.com")).thenReturn(Optional.of(docente));
        when(docente.getPasswordHash()).thenReturn(null);
        when(docente.getId()).thenReturn(7L);

        assertThrows(ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(new LoginRequest("docente@educoders.com", "cualquiera")));

        assertEventoDeSesion(auditoriaRegistrada(), "Intento fallido de inicio de sesión", 7L,
                "Sin sesión", "Sin sesión");
    }

    @Test
    void cuentaInactivaGeneraIntentoFallidoDelTitular() {
        when(usuarioRepository.findByEmail("docente@educoders.com")).thenReturn(Optional.of(docente));
        when(docente.getPasswordHash()).thenReturn("hash");
        when(docente.isActivo()).thenReturn(false);
        when(docente.getId()).thenReturn(7L);
        when(passwordEncoder.matches("clave123", "hash")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(new LoginRequest("docente@educoders.com", "clave123")));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEventoDeSesion(auditoriaRegistrada(), "Intento fallido de inicio de sesión", 7L,
                "Sin sesión", "Sin sesión");
    }

    @Test
    void siLaAuditoriaDelIntentoFallidoFallaIgualSeResponde401() {
        when(usuarioRepository.findByEmail("nadie@educoders.com")).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("BD caída")).when(auditoriaService).registrarAuditoria(any());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(new LoginRequest("nadie@educoders.com", "clave123")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void cerrarSesionQuedaEnLaAuditoriaDelTitular() {
        autenticacionService.cerrarSesion(7L);

        assertEventoDeSesion(auditoriaRegistrada(), "Cierre de sesión", 7L, "Sesión iniciada", "Sin sesión");
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
            String correo = "usuario" + i + "@educoders.com";

            when(usuarioRepository.findByEmail(correo)).thenReturn(Optional.of(usuario));

            when(usuario.getPasswordHash()).thenReturn("hash");
            when(usuario.isActivo()).thenReturn(true);

            when(passwordEncoder.matches("clave123", "hash"))
                    .thenReturn(true);

            when(jwtService.generarToken(usuario, rolEsperado))
                    .thenReturn("token-" + rolEsperado);

            LoginResponse respuesta = autenticacionService.iniciarSesion(
                    new LoginRequest(correo, "clave123")
            );

            assertEquals(rolEsperado, respuesta.usuario().rol());
            assertEquals("token-" + rolEsperado, respuesta.token());
        }
    }
}
