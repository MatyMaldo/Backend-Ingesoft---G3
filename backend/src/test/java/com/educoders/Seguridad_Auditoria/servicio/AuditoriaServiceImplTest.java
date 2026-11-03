package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.dominio.Auditoria;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.AuditoriaRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceImplTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

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