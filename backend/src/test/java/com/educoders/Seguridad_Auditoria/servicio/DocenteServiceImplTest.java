package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarDocenteRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El administrador no define contraseñas: el docente nace sin contraseña y la establece él mismo
 * con el código de verificación (HU-SAT-03).
 */
@ExtendWith(MockitoExtension.class)
class DocenteServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private DocenteServiceImpl service;

    @Test
    void registrarDocente_creaLaCuentaActivaYSinContrasena() {
        when(docenteRepository.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarDocente(new RegistrarDocenteRequest(
                "Marco", "Docente", null, "11000011", "marco@example.com", "910000011"));

        ArgumentCaptor<Docente> captor = ArgumentCaptor.forClass(Docente.class);
        verify(docenteRepository).save(captor.capture());
        assertNull(captor.getValue().getPasswordHash());
        assertEquals("marco", captor.getValue().getUsername());
        assertTrue(captor.getValue().isActivo());
    }

    @Test
    void registrarDocente_dejaUnRegistroDeAuditoriaSinContrasena() {
        when(docenteRepository.save(any(Docente.class))).thenAnswer(inv -> {
            Docente guardado = inv.getArgument(0);
            guardado.setId(5L);
            return guardado;
        });

        service.registrarDocente(new RegistrarDocenteRequest(
                "Marco", "Docente", null, "11000011", "marco@example.com", "910000011"));

        ArgumentCaptor<RegistrarAuditoriaRequestDTO> captor = ArgumentCaptor.forClass(RegistrarAuditoriaRequestDTO.class);
        verify(auditoriaService).registrarAuditoria(captor.capture());
        RegistrarAuditoriaRequestDTO auditoria = captor.getValue();
        assertEquals(CategoriaAuditoria.USUARIO, auditoria.getCategoria());
        assertEquals(TipoAccionAuditoria.CREAR, auditoria.getAccion());
        assertEquals("Registro de usuario", auditoria.getDescripcion());
        assertEquals("Usuario", auditoria.getEntidad());
        assertEquals(5L, auditoria.getEntidadId());
        assertTrue(auditoria.getDetalles().stream().anyMatch(
                d -> d.getCampo().equals("Rol") && d.getValorNuevo().equals("Docente") && d.getValorAnterior() == null));
        assertTrue(auditoria.getDetalles().stream().noneMatch(
                d -> d.getCampo().toLowerCase().contains("contrase")));
    }

    @Test
    void registrarDocente_siLaAuditoriaFallaElErrorSePropagaParaRevertirElAlta() {
        when(docenteRepository.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("BD caída")).when(auditoriaService).registrarAuditoria(any());

        assertThrows(IllegalStateException.class, () -> service.registrarDocente(new RegistrarDocenteRequest(
                "Marco", "Docente", null, "11000011", "marco@example.com", "910000011")));
    }
}
