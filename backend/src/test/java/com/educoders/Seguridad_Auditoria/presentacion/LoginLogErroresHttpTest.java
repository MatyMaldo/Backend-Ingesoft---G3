package com.educoders.Seguridad_Auditoria.presentacion;

import com.educoders.Infraestructura_Servicios.enums.Modulo;
import com.educoders.Infraestructura_Servicios.enums.TipoError;
import com.educoders.Infraestructura_Servicios.exception.ManejadorGlobalExcepciones;
import com.educoders.Infraestructura_Servicios.servicio.ModuloResolver;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.LogError;
import com.educoders.Seguridad_Auditoria.persistencia.LogErrorRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.controladores.AutenticacionController;
import com.educoders.Seguridad_Auditoria.seguridad.JwtAuthenticationFilter;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import com.educoders.Seguridad_Auditoria.servicio.AutenticacionServiceImpl;
import com.educoders.Seguridad_Auditoria.servicio.JwtService;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorServiceImpl;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorWriter;
import com.educoders.Seguridad_Auditoria.servicio.RestablecimientoPasswordService;
import com.educoders.config.SeguridadConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-SAT-07 Esc. 12 de punta a punta por HTTP: si la auditoría del inicio de sesión no puede guardarse, no hay
 * sesión, el usuario ve "Se registró un error inesperado" y el error queda en el log de errores (HU-SAT-06) en el
 * módulo Autenticación. Los casos controlados (credenciales incorrectas, cuenta inactiva) no se registran en el log.
 *
 * <p>Usa el controlador, el servicio de autenticación, el manejador global de excepciones, el servicio del log y el
 * resolvedor de módulo reales. Solo se reemplaza lo que toca la base de datos (repositorios, auditoría y la escritura
 * del log), porque el log de errores no se puede borrar: una prueba contra la BD real dejaría filas permanentes.
 */
@WebMvcTest(AutenticacionController.class)
@Import({
        SeguridadConfig.class,
        JwtAuthenticationFilter.class,
        AutenticacionServiceImpl.class,
        LogErrorServiceImpl.class,
        ModuloResolver.class,
        ManejadorGlobalExcepciones.class
})
@TestPropertySource(properties = "server.port=0")
class LoginLogErroresHttpTest {

    private static final String CORREO = "docente@educoders.com";
    private static final String CLAVE = "clave123";

    @Autowired MockMvc mvc;

    @MockBean UsuarioRepository usuarioRepository;
    @MockBean JwtService jwtService;
    @MockBean AuditoriaService auditoriaService;
    @MockBean PlatformTransactionManager transactionManager;
    @MockBean RestablecimientoPasswordService restablecimientoPasswordService;
    @MockBean LogErrorRepository logErrorRepository;
    @MockBean LogErrorWriter logErrorWriter;

    private Docente docente;

    @BeforeEach
    void configurar() {
        docente = mock(Docente.class);
        when(docente.getPasswordHash()).thenReturn(new BCryptPasswordEncoder().encode(CLAVE));
        when(docente.getId()).thenReturn(7L);
        when(docente.getNombre()).thenReturn("Carlos");
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(docente));
        when(jwtService.generarToken(docente, "DOCENTE")).thenReturn("token-prueba");
    }

    private String cuerpo(String correo, String clave) {
        return "{\"identificador\":\"" + correo + "\",\"password\":\"" + clave + "\"}";
    }

    @Test
    void siLaAuditoriaFallaNoHaySesionYElErrorQuedaEnElLogDeAutenticacion() throws Exception {
        when(docente.isActivo()).thenReturn(true);
        doThrow(new IllegalStateException("BD caída")).when(auditoriaService).registrarAuditoria(any());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo(CORREO, CLAVE)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Se registró un error inesperado"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.datos").doesNotExist());

        ArgumentCaptor<LogError> captor = ArgumentCaptor.forClass(LogError.class);
        verify(logErrorWriter).guardar(captor.capture());
        LogError registrado = captor.getValue();

        assertEquals(Modulo.AUTENTICACION, registrado.getModulo());
        assertEquals(TipoError.EXCEPCION_NO_CONTROLADA, registrado.getTipoError());
        assertEquals("/api/v1/auth/login", registrado.getEndpoint());
        assertEquals("POST", registrado.getMetodoHttp());
        assertEquals("BD caída", registrado.getMensaje());
        assertNull(registrado.getUsuario(), "sin sesión iniciada el usuario es 'No determinado'");
    }

    @Test
    void credencialesIncorrectasSonUnCasoControladoYNoVanAlLogDeErrores() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo(CORREO, "mala")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Correo o contraseña incorrectos"));

        verify(logErrorWriter, never()).guardar(any());
    }

    @Test
    void cuentaInactivaEsUnCasoControladoYNoVaAlLogDeErrores() throws Exception {
        when(docente.isActivo()).thenReturn(false);

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo(CORREO, CLAVE)))
                .andExpect(status().isForbidden());

        verify(logErrorWriter, never()).guardar(any());
    }
}
