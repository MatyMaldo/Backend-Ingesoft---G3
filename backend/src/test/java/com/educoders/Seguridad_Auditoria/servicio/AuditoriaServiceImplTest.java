package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Auditoria;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.AuditoriaRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.AuditoriaResponseDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceImplTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuditoriaServiceImpl auditoriaService;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private RegistrarAuditoriaRequestDTO dtoConUsuario(Long usuarioId) {
        return new RegistrarAuditoriaRequestDTO(
                usuarioId,
                CategoriaAuditoria.HORARIOS,
                TipoAccionAuditoria.CREAR,
                "BloqueHorario",
                1L,
                "prueba",
                List.of()
        );
    }

    @Test
    void registrarAuditoria_tomaIdDelPrincipalUsuario() {
        Usuario docente = new Docente();
        docente.setId(7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(docente, null, List.of()));

        when(auditoriaRepository.saveAndFlush(any(Auditoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        auditoriaService.registrarAuditoria(dtoConUsuario(null));

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).saveAndFlush(captor.capture());
        assertEquals(7L, captor.getValue().getUsuarioId());
    }

    @Test
    void registrarAuditoria_compatibilidadPrincipalString() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("12", null, List.of()));

        when(auditoriaRepository.saveAndFlush(any(Auditoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        auditoriaService.registrarAuditoria(dtoConUsuario(null));

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).saveAndFlush(captor.capture());
        assertEquals(12L, captor.getValue().getUsuarioId());
    }

    @Test
    void registrarAuditoria_sinAutenticacion_usaIdProporcionado() {
        SecurityContextHolder.clearContext();

        when(auditoriaRepository.saveAndFlush(any(Auditoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        auditoriaService.registrarAuditoria(dtoConUsuario(99L));

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).saveAndFlush(captor.capture());
        assertEquals(99L, captor.getValue().getUsuarioId());
    }

    private Auditoria crearAuditoria(Long usuarioId) {
        Auditoria auditoria = new Auditoria();
        auditoria.setId(1L);
        auditoria.setFechaHora(LocalDateTime.now());
        auditoria.setUsuarioId(usuarioId);
        auditoria.setCategoria(CategoriaAuditoria.HORARIOS);
        auditoria.setAccion(TipoAccionAuditoria.CREAR);
        auditoria.setEntidad("BloqueHorario");
        auditoria.setEntidadId(10L);
        auditoria.setDescripcion("prueba");
        return auditoria;
    }

    private Administrador crearAdmin() {
        Administrador admin = new Administrador();
        admin.setId(1L);
        admin.setNombre("Admin");
        admin.setApellidoPaterno("Sistema");
        return admin;
    }

    @Test
    void listarAuditorias_incluyeUsuarioResueltoEnLote() {
        Auditoria auditoria = crearAuditoria(1L);
        PageRequest pageable = PageRequest.of(0, 10);
        when(auditoriaRepository.buscarConFiltros(null, null, null, null, "%", pageable))
                .thenReturn(new PageImpl<>(List.of(auditoria), pageable, 1));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(crearAdmin()));

        Page<AuditoriaResponseDTO> resultado =
                auditoriaService.listarAuditorias(null, null, null, null, null, pageable);

        AuditoriaResponseDTO dto = resultado.getContent().get(0);
        assertEquals(1L, dto.getUsuarioId());
        assertEquals(1L, dto.getUsuario().id());
        assertEquals("Admin Sistema", dto.getUsuario().nombre());
        assertEquals("ADMINISTRADOR", dto.getUsuario().rol());
        verify(usuarioRepository).findAllById(any());
    }

    @Test
    void listarAuditorias_traduceLaBusquedaAUnPatronLikeEscapado() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(auditoriaRepository.buscarConFiltros(null, null, null, null, "%100!%!_pago%", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<AuditoriaResponseDTO> resultado =
                auditoriaService.listarAuditorias(null, null, null, null, "  100%_Pago ", pageable);

        assertEquals(0, resultado.getTotalElements());
        verify(auditoriaRepository).buscarConFiltros(null, null, null, null, "%100!%!_pago%", pageable);
    }

    @Test
    void listarAuditorias_busquedaEnBlancoNoFiltra() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(auditoriaRepository.buscarConFiltros(null, null, null, null, "%", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        auditoriaService.listarAuditorias(null, null, null, null, "   ", pageable);

        verify(auditoriaRepository).buscarConFiltros(null, null, null, null, "%", pageable);
    }

    @Test
    void obtenerPorId_incluyeUsuario() {
        Auditoria auditoria = crearAuditoria(1L);
        when(auditoriaRepository.findById(1L)).thenReturn(Optional.of(auditoria));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(crearAdmin()));

        AuditoriaResponseDTO dto = auditoriaService.obtenerPorId(1L);

        assertEquals("Admin Sistema", dto.getUsuario().nombre());
        assertEquals("ADMINISTRADOR", dto.getUsuario().rol());
        verify(usuarioRepository).findById(1L);
    }

    @Test
    void listarAuditorias_sinUsuarioUsaSistema() {
        Auditoria auditoria = crearAuditoria(null);
        PageRequest pageable = PageRequest.of(0, 10);
        when(auditoriaRepository.buscarConFiltros(null, null, null, null, "%", pageable))
                .thenReturn(new PageImpl<>(List.of(auditoria), pageable, 1));

        Page<AuditoriaResponseDTO> resultado =
                auditoriaService.listarAuditorias(null, null, null, null, null, pageable);

        AuditoriaResponseDTO dto = resultado.getContent().get(0);
        assertNull(dto.getUsuarioId());
        assertEquals("Sistema", dto.getUsuario().nombre());
        assertEquals("SISTEMA", dto.getUsuario().rol());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void listarAuditorias_usuarioNoEncontradoUsaFallback() {
        Auditoria auditoria = crearAuditoria(99L);
        PageRequest pageable = PageRequest.of(0, 10);
        when(auditoriaRepository.buscarConFiltros(null, null, null, null, "%", pageable))
                .thenReturn(new PageImpl<>(List.of(auditoria), pageable, 1));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of());

        Page<AuditoriaResponseDTO> resultado =
                auditoriaService.listarAuditorias(null, null, null, null, null, pageable);

        AuditoriaResponseDTO dto = resultado.getContent().get(0);
        assertEquals("Usuario #99", dto.getUsuario().nombre());
        assertNull(dto.getUsuario().rol());
    }

    @Test
    void obtenerHistorialPorEntidad_incluyeUsuario() {
        Auditoria auditoria = crearAuditoria(1L);
        PageRequest pageable = PageRequest.of(0, 10);
        when(auditoriaRepository.findByEntidadAndEntidadIdOrderByFechaHoraAsc(
                "BloqueHorario", 10L, pageable))
                .thenReturn(new PageImpl<>(List.of(auditoria), pageable, 1));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(crearAdmin()));

        Page<AuditoriaResponseDTO> resultado =
                auditoriaService.obtenerHistorialPorEntidad("BloqueHorario", 10L, pageable);

        assertEquals("Admin Sistema", resultado.getContent().get(0).getUsuario().nombre());
    }

    @Test
    void registrarAuditoria_conAutorFijoIgnoraAlUsuarioDelToken() {
        Usuario docente = new Docente();
        docente.setId(7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(docente, null, List.of()));
        when(auditoriaRepository.saveAndFlush(any(Auditoria.class))).thenAnswer(inv -> inv.getArgument(0));

        auditoriaService.registrarAuditoria(dtoConUsuario(null).conAutorFijo());
        auditoriaService.registrarAuditoria(dtoConUsuario(3L).conAutorFijo());

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository, times(2)).saveAndFlush(captor.capture());
        assertNull(captor.getAllValues().get(0).getUsuarioId());
        assertEquals(3L, captor.getAllValues().get(1).getUsuarioId());
    }

    @Test
    void listarAuditorias_loginSinUsuarioMuestraAutorNoDeterminado() {
        Auditoria intento = crearAuditoria(null);
        intento.setAccion(TipoAccionAuditoria.LOGIN);
        PageRequest pageable = PageRequest.of(0, 10);
        when(auditoriaRepository.buscarConFiltros(null, null, null, null, "%", pageable))
                .thenReturn(new PageImpl<>(List.of(intento), pageable, 1));

        AuditoriaResponseDTO dto = auditoriaService
                .listarAuditorias(null, null, null, null, null, pageable).getContent().get(0);

        assertNull(dto.getUsuarioId());
        assertEquals("No determinado", dto.getUsuario().nombre());
        assertEquals("NO_DETERMINADO", dto.getUsuario().rol());
    }

    @Test
    void registrarAuditoria_siNoPuedeGuardarse_propagaElErrorParaCancelarLaOperacion() {
        when(auditoriaRepository.saveAndFlush(any(Auditoria.class)))
                .thenThrow(new IllegalStateException("BD caída"));

        assertThrows(IllegalStateException.class, () -> auditoriaService.registrarAuditoria(dtoConUsuario(1L)));
    }

    @Test
    void registrarAuditoria_exigeLaTransaccionDeLaOperacionAuditada() throws NoSuchMethodException {
        Transactional transaccional = AuditoriaServiceImpl.class
                .getMethod("registrarAuditoria", RegistrarAuditoriaRequestDTO.class)
                .getAnnotation(Transactional.class);

        assertEquals(Propagation.MANDATORY, transaccional.propagation());
    }

    @Test
    void registrarAuditoria_sinAutenticacionNiId_guardaNull() {
        SecurityContextHolder.clearContext();

        when(auditoriaRepository.saveAndFlush(any(Auditoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        auditoriaService.registrarAuditoria(dtoConUsuario(null));

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).saveAndFlush(captor.capture());
        assertNull(captor.getValue().getUsuarioId());
    }
}