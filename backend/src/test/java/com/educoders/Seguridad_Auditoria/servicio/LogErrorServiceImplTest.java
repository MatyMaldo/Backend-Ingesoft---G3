package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.Modulo;
import com.educoders.Infraestructura_Servicios.enums.TipoError;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.servicio.ModuloResolver;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.LogError;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.LogErrorRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LogErrorResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogErrorServiceImplTest {

    @Mock private LogErrorRepository logErrorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LogErrorWriter logErrorWriter;
    @Mock private ModuloResolver moduloResolver;

    @InjectMocks
    private LogErrorServiceImpl service;

    private final Pageable pageable = PageRequest.of(0, 15);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrarError_guardaModuloResuelto() {
        when(moduloResolver.resolver("/api/v1/pagos")).thenReturn(Modulo.PAGOS);

        service.registrarError("PagoService", TipoError.EXCEPCION_NO_CONTROLADA, "boom", "detalle",
                "/api/v1/pagos", "POST", (short) 500, null);

        ArgumentCaptor<LogError> captor = ArgumentCaptor.forClass(LogError.class);
        verify(logErrorWriter).guardar(captor.capture());
        assertEquals(Modulo.PAGOS, captor.getValue().getModulo());
        assertEquals("/api/v1/pagos", captor.getValue().getEndpoint());
    }

    @Test
    void registrarError_noPropagaSiFallaLaEscritura() {
        when(moduloResolver.resolver(any())).thenReturn(Modulo.OTROS);
        doThrow(new RuntimeException("db down")).when(logErrorWriter).guardar(any());

        assertDoesNotThrow(() -> service.registrarError("origen", TipoError.EXCEPCION_NO_CONTROLADA, "mensaje", null,
                "/api/v1/x", "GET", (short) 500, null));
    }

    @Test
    void listarErrores_filtraPorEtiqueta() {
        when(logErrorRepository.buscarConFiltros(null, null, Modulo.NOTAS, "%", pageable))
                .thenReturn(Page.empty(pageable));

        service.listarErrores(null, null, "Notas", null, pageable);

        verify(logErrorRepository).buscarConFiltros(null, null, Modulo.NOTAS, "%", pageable);
    }

    @Test
    void listarErrores_filtraPorNombreEnum() {
        when(logErrorRepository.buscarConFiltros(null, null, Modulo.PAGOS, "%", pageable))
                .thenReturn(Page.empty(pageable));

        service.listarErrores(null, null, "pagos", null, pageable);

        verify(logErrorRepository).buscarConFiltros(null, null, Modulo.PAGOS, "%", pageable);
    }

    @Test
    void listarErrores_moduloInvalidoLanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class,
                () -> service.listarErrores(null, null, "xyz", null, pageable));
        verifyNoInteractions(logErrorRepository);
    }

    @Test
    void listarErrores_sinModuloNoFiltra() {
        when(logErrorRepository.buscarConFiltros(null, null, null, "%", pageable))
                .thenReturn(Page.empty(pageable));

        service.listarErrores(null, null, null, null, pageable);

        verify(logErrorRepository).buscarConFiltros(null, null, null, "%", pageable);
    }

    @Test
    void listarErrores_busquedaSeConvierteEnPatronEnMinusculas() {
        when(logErrorRepository.buscarConFiltros(null, null, null, "%timeout%", pageable))
                .thenReturn(Page.empty(pageable));

        service.listarErrores(null, null, null, "  TimeOut ", pageable);

        verify(logErrorRepository).buscarConFiltros(null, null, null, "%timeout%", pageable);
    }

    @Test
    void listarErrores_busquedaEscapaComodines() {
        when(logErrorRepository.buscarConFiltros(null, null, null, "%100!%!_x%", pageable))
                .thenReturn(Page.empty(pageable));

        service.listarErrores(null, null, null, "100%_x", pageable);

        verify(logErrorRepository).buscarConFiltros(null, null, null, "%100!%!_x%", pageable);
    }

    @Test
    void mapeoResponseIncluyeModuloYEtiqueta() {
        LogError logError = new LogError(LocalDateTime.now(), null, Modulo.NOTAS,
                TipoError.EXCEPCION_NO_CONTROLADA, "mensaje", null, "/api/v1/evaluaciones", "POST", (short) 500);

        LogErrorResponse response = LogErrorResponse.desdeEntidad(logError);

        assertEquals(Modulo.NOTAS, response.modulo());
        assertEquals("Notas", response.moduloEtiqueta());
    }

    @Test
    void mapeoResponseUsuarioNoDeterminadoEnFlujoSinSesion() {
        LogError logError = new LogError(LocalDateTime.now(), null, Modulo.USUARIOS,
                TipoError.NOTIFICACION_CORREO, "no se envió el código", null,
                "/api/v1/auth/olvide-contrasena", "POST", (short) 500);

        LogErrorResponse response = LogErrorResponse.desdeEntidad(logError);

        assertEquals("No determinado", response.nombreUsuario());
        assertEquals("Notificación por correo", response.tipoError());
    }

    @Test
    void mapeoResponseUsuarioSistemaEnProcesoAutomatico() {
        LogError logError = new LogError(LocalDateTime.now(), null, Modulo.MATRICULA,
                TipoError.PROCESO_AUTOMATICO, "falló el cierre del periodo", null,
                "/api/v1/matriculas", null, null);

        LogErrorResponse response = LogErrorResponse.desdeEntidad(logError);

        assertEquals("Sistema", response.nombreUsuario());
        assertEquals("Proceso automático", response.tipoError());
    }

    @Test
    void mapeoResponseConUsuarioMuestraSuNombre() {
        Docente docente = new Docente();
        docente.setId(7L);
        docente.setNombre("Ana");
        docente.setApellidoPaterno("Rojas");
        LogError logError = new LogError(LocalDateTime.now(), docente, Modulo.NOTAS,
                TipoError.EXCEPCION_NO_CONTROLADA, "boom", null, "/api/v1/evaluaciones", "POST", (short) 500);

        LogErrorResponse response = LogErrorResponse.desdeEntidad(logError);

        assertEquals("Ana Rojas", response.nombreUsuario());
        assertEquals(7L, response.usuarioId());
    }

    @Test
    void registrarError_tomaUsuarioDelSecurityContext() {
        Docente docente = new Docente();
        docente.setId(7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(docente, null, List.of()));

        when(moduloResolver.resolver(any())).thenReturn(Modulo.OTROS);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(docente));

        service.registrarError("origen", TipoError.EXCEPCION_NO_CONTROLADA, "msg", null,
                "/api/v1/xyz", "GET", (short) 404, null);

        ArgumentCaptor<LogError> captor = ArgumentCaptor.forClass(LogError.class);
        verify(logErrorWriter).guardar(captor.capture());
        assertEquals(7L, captor.getValue().getUsuario().getId());
    }

    @Test
    void registrarError_usaIdExplicitoSobreSecurityContext() {
        Docente autenticado = new Docente();
        autenticado.setId(7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(autenticado, null, List.of()));

        Docente explicito = new Docente();
        explicito.setId(9L);

        when(moduloResolver.resolver(any())).thenReturn(Modulo.OTROS);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(explicito));

        service.registrarError("origen", TipoError.EXCEPCION_NO_CONTROLADA, "msg", null,
                "/api/v1/xyz", "GET", (short) 500, 9L);

        ArgumentCaptor<LogError> captor = ArgumentCaptor.forClass(LogError.class);
        verify(logErrorWriter).guardar(captor.capture());
        assertEquals(9L, captor.getValue().getUsuario().getId());
    }

    @Test
    void registrarError_sinContextoNiId_guardaUsuarioNull() {
        SecurityContextHolder.clearContext();
        when(moduloResolver.resolver(any())).thenReturn(Modulo.OTROS);

        service.registrarError("origen", TipoError.EXCEPCION_NO_CONTROLADA, "msg", null,
                "/api/v1/xyz", "GET", (short) 500, null);

        ArgumentCaptor<LogError> captor = ArgumentCaptor.forClass(LogError.class);
        verify(logErrorWriter).guardar(captor.capture());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getUsuario());
    }
}