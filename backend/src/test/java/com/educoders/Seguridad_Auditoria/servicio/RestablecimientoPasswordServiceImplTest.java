package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.servicio.ServicioNotificacionEmail;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.TokenRestablecimiento;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.TokenRestablecimientoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestablecimientoPasswordServiceImplTest {

    @Mock private TokenRestablecimientoRepository tokenRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ServicioNotificacionEmail servicioNotificacionEmail;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private RestablecimientoPasswordServiceImpl service;

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(service, "expiracionMinutos", 15L);
        ReflectionTestUtils.setField(service, "cooldownSegundos", 60L);
        ReflectionTestUtils.setField(service, "maxIntentos", 5);
    }

    private Usuario usuario() {
        Administrador usuario = new Administrador();
        usuario.setId(1L);
        usuario.setNombre("Ana");
        usuario.setApellidoPaterno("Torres");
        usuario.setEmail("ana@colegio.com");
        usuario.setActivo(true);
        return usuario;
    }

    private TokenRestablecimiento token(Usuario usuario, String hash,
                                        LocalDateTime expiracion, boolean usado, int intentos) {
        TokenRestablecimiento token = new TokenRestablecimiento();
        token.setId(10L);
        token.setUsuario(usuario);
        token.setCodigoHash(hash);
        token.setFechaGeneracion(LocalDateTime.now().minusMinutes(1));
        token.setFechaExpiracion(expiracion);
        token.setUsado(usado);
        token.setIntentos(intentos);
        return token;
    }

    @Test
    void solicitarCodigo_usuarioActivo_generaTokenYEnviaCorreo() {
        Usuario usuario = usuario();
        when(usuarioRepository.findByEmail("ana@colegio.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findTopByUsuarioIdOrderByFechaGeneracionDesc(1L))
                .thenReturn(Optional.empty());
        when(tokenRepository.findByUsuarioIdAndUsadoFalse(1L)).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-codigo");

        service.solicitarCodigo("ana@colegio.com");

        ArgumentCaptor<TokenRestablecimiento> captor =
                ArgumentCaptor.forClass(TokenRestablecimiento.class);
        verify(tokenRepository).save(captor.capture());
        TokenRestablecimiento guardado = captor.getValue();
        assertEquals("hash-codigo", guardado.getCodigoHash());
        assertFalse(guardado.isUsado());
        assertEquals(0, guardado.getIntentos());
        assertEquals(1L, guardado.getUsuario().getId());
        assertTrue(guardado.getFechaExpiracion().isAfter(LocalDateTime.now()));

        verify(servicioNotificacionEmail).enviarCorreoRestablecimientoPassword(
                eq("ana@colegio.com"), anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void solicitarCodigo_usuarioInexistente_noHaceNada() {
        when(usuarioRepository.findByEmail("nadie@colegio.com")).thenReturn(Optional.empty());

        service.solicitarCodigo("nadie@colegio.com");

        verifyNoInteractions(tokenRepository, servicioNotificacionEmail);
    }

    @Test
    void solicitarCodigo_enCooldown_noGeneraNuevoCodigo() {
        Usuario usuario = usuario();
        TokenRestablecimiento reciente = token(usuario, "hash", LocalDateTime.now().plusMinutes(10),
                false, 0);
        reciente.setFechaGeneracion(LocalDateTime.now().minusSeconds(10));

        when(usuarioRepository.findByEmail("ana@colegio.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findTopByUsuarioIdOrderByFechaGeneracionDesc(1L))
                .thenReturn(Optional.of(reciente));

        service.solicitarCodigo("ana@colegio.com");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(servicioNotificacionEmail);
    }

    @Test
    void verificarCodigo_correcto_devuelveTrueSinConsumir() {
        Usuario usuario = usuario();
        TokenRestablecimiento token = token(usuario, "hash", LocalDateTime.now().plusMinutes(10),
                false, 0);
        when(usuarioRepository.findByEmail("ana@colegio.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findTopByUsuarioIdAndUsadoFalseOrderByFechaGeneracionDesc(1L))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        assertTrue(service.verificarCodigo("ana@colegio.com", "123456"));
        assertFalse(token.isUsado());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void verificarCodigo_incorrecto_incrementaIntentos() {
        Usuario usuario = usuario();
        TokenRestablecimiento token = token(usuario, "hash", LocalDateTime.now().plusMinutes(10),
                false, 0);
        when(usuarioRepository.findByEmail("ana@colegio.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findTopByUsuarioIdAndUsadoFalseOrderByFechaGeneracionDesc(1L))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("000000", "hash")).thenReturn(false);

        assertFalse(service.verificarCodigo("ana@colegio.com", "000000"));
        assertEquals(1, token.getIntentos());
        assertFalse(token.isUsado());
        verify(tokenRepository).save(token);
    }

    @Test
    void verificarCodigo_hashAntiguoNoReconocido_devuelveFalseSinRomper() {
        Usuario usuario = usuario();
        TokenRestablecimiento token = token(usuario, "token-plano-antiguo",
                LocalDateTime.now().plusMinutes(10), false, 0);
        when(usuarioRepository.findByEmail("ana@colegio.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findTopByUsuarioIdAndUsadoFalseOrderByFechaGeneracionDesc(1L))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("123456", "token-plano-antiguo"))
                .thenThrow(new IllegalArgumentException("Hash inválido"));

        assertFalse(service.verificarCodigo("ana@colegio.com", "123456"));
        assertEquals(1, token.getIntentos());
    }

    @Test
    void verificarCodigo_expirado_marcaUsado() {
        Usuario usuario = usuario();
        TokenRestablecimiento token = token(usuario, "hash", LocalDateTime.now().minusMinutes(1),
                false, 0);
        when(usuarioRepository.findByEmail("ana@colegio.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findTopByUsuarioIdAndUsadoFalseOrderByFechaGeneracionDesc(1L))
                .thenReturn(Optional.of(token));

        assertFalse(service.verificarCodigo("ana@colegio.com", "123456"));
        assertTrue(token.isUsado());
        verify(tokenRepository).save(token);
    }

    @Test
    void verificarCodigo_maximoIntentos_marcaUsado() {
        Usuario usuario = usuario();
        TokenRestablecimiento token = token(usuario, "hash", LocalDateTime.now().plusMinutes(10),
                false, 5);
        when(usuarioRepository.findByEmail("ana@colegio.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findTopByUsuarioIdAndUsadoFalseOrderByFechaGeneracionDesc(1L))
                .thenReturn(Optional.of(token));

        assertFalse(service.verificarCodigo("ana@colegio.com", "123456"));
        assertTrue(token.isUsado());
        verify(tokenRepository).save(token);
    }

    @Test
    void restablecerPassword_correcto_actualizaYConsumeCodigo() {
        Usuario usuario = usuario();
        TokenRestablecimiento token = token(usuario, "hash", LocalDateTime.now().plusMinutes(10),
                false, 0);
        when(usuarioRepository.findByEmail("ana@colegio.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findTopByUsuarioIdAndUsadoFalseOrderByFechaGeneracionDesc(1L))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(passwordEncoder.encode("NuevaClave123")).thenReturn("nuevo-hash");
        when(tokenRepository.findByUsuarioIdAndUsadoFalse(1L)).thenReturn(List.of());

        service.restablecerPassword("ana@colegio.com", "123456", "NuevaClave123");

        assertEquals("nuevo-hash", usuario.getPasswordHash());
        assertTrue(token.isUsado());
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).save(token);
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void restablecerPassword_incorrecto_noCambiaPassword() {
        Usuario usuario = usuario();
        TokenRestablecimiento token = token(usuario, "hash", LocalDateTime.now().plusMinutes(10),
                false, 0);
        when(usuarioRepository.findByEmail("ana@colegio.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findTopByUsuarioIdAndUsadoFalseOrderByFechaGeneracionDesc(1L))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("000000", "hash")).thenReturn(false);

        assertThrows(ReglaNegocioException.class,
                () -> service.restablecerPassword("ana@colegio.com", "000000", "NuevaClave123"));

        assertNull(usuario.getPasswordHash());
        assertEquals(1, token.getIntentos());
        verify(usuarioRepository, never()).save(any());
        verify(tokenRepository).save(token);
    }
}
