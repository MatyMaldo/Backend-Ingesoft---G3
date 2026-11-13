package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.BimestreRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ActualizarUsuarioRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SeccionRepository seccionRepository;
    @Mock private BimestreRepository bimestreRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private UsuarioServiceImpl service;

    private void completarDatos(Usuario usuario, Long id, String dni) {
        usuario.setId(id);
        usuario.setNombre("Nombre" + id);
        usuario.setApellidoPaterno("Apellido");
        usuario.setDni(dni);
        usuario.setUsername("usuario" + id);
        usuario.setEmail("usuario" + id + "@example.com");
        usuario.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 8, 0));
        usuario.setActivo(true);
    }

    private Estudiante estudiante(Long id, String codigo) {
        Estudiante estudiante = new Estudiante();
        completarDatos(estudiante, id, "5000000" + id);
        estudiante.setCodigo(codigo);
        return estudiante;
    }

    private Docente docente(Long id) {
        Docente docente = new Docente();
        completarDatos(docente, id, "2000000" + id);
        return docente;
    }

    @Test
    void listar_incluyeElCodigoDeLosEstudiantesYNuloEnLosDemasTipos() {
        PageRequest pagina = PageRequest.of(0, 100);
        when(usuarioRepository.findAll(pagina))
                .thenReturn(new PageImpl<>(List.of(estudiante(1L, "EST016"), docente(2L), estudiante(3L, "EST017"))));

        Page<UsuarioResponse> resultado = service.listar(null, null, pagina);

        assertEquals(List.of("ESTUDIANTE", "DOCENTE", "ESTUDIANTE"),
                resultado.getContent().stream().map(UsuarioResponse::tipoUsuario).toList());
        assertEquals("EST016", resultado.getContent().get(0).codigo());
        assertNull(resultado.getContent().get(1).codigo());
        assertEquals("EST017", resultado.getContent().get(2).codigo());
    }

    @Test
    void obtener_devuelveElCodigoDelEstudiante() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(estudiante(1L, "EST020")));

        assertEquals("EST020", service.obtener(1L).codigo());
    }

    @Test
    void obtener_noInformaCodigoParaUnDocente() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(docente(2L)));

        assertNull(service.obtener(2L).codigo());
    }

    private RegistrarAuditoriaRequestDTO auditoriaRegistrada() {
        ArgumentCaptor<RegistrarAuditoriaRequestDTO> captor = ArgumentCaptor.forClass(RegistrarAuditoriaRequestDTO.class);
        verify(auditoriaService).registrarAuditoria(captor.capture());
        return captor.getValue();
    }

    @Test
    void actualizar_registraSoloLosCamposCambiadosConSuValorAntesYDespues() {
        Docente docente = docente(2L);
        docente.setTelefono("900000001");
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(docente));
        when(usuarioRepository.save(docente)).thenReturn(docente);

        service.actualizar(2L, new ActualizarUsuarioRequest("Ana", null, null, null, null, "955555555"));

        RegistrarAuditoriaRequestDTO auditoria = auditoriaRegistrada();
        assertEquals(TipoAccionAuditoria.ACTUALIZAR, auditoria.getAccion());
        assertEquals("Edición de usuario", auditoria.getDescripcion());
        assertEquals(2L, auditoria.getEntidadId());
        assertEquals(2, auditoria.getDetalles().size());
        assertEquals("Nombre", auditoria.getDetalles().get(0).getCampo());
        assertEquals("Nombre2", auditoria.getDetalles().get(0).getValorAnterior());
        assertEquals("Ana", auditoria.getDetalles().get(0).getValorNuevo());
        assertEquals("Teléfono", auditoria.getDetalles().get(1).getCampo());
        assertEquals("900000001", auditoria.getDetalles().get(1).getValorAnterior());
        assertEquals("955555555", auditoria.getDetalles().get(1).getValorNuevo());
    }

    @Test
    void actualizar_sinCambiosRealesNoGeneraRegistro() {
        Docente docente = docente(2L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(docente));
        when(usuarioRepository.save(docente)).thenReturn(docente);

        service.actualizar(2L, new ActualizarUsuarioRequest("Nombre2", null, null, null, null, null));

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void desactivar_registraElCambioDeEstado() {
        Docente docente = docente(2L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(docente));
        when(seccionRepository.findByTutorId(2L)).thenReturn(List.of());
        when(usuarioRepository.save(docente)).thenReturn(docente);

        service.desactivar(2L);

        RegistrarAuditoriaRequestDTO auditoria = auditoriaRegistrada();
        assertEquals(TipoAccionAuditoria.CAMBIO_ESTADO, auditoria.getAccion());
        assertEquals("Desactivación de usuario", auditoria.getDescripcion());
        assertEquals("Activo", auditoria.getDetalles().get(0).getValorAnterior());
        assertEquals("Inactivo", auditoria.getDetalles().get(0).getValorNuevo());
    }

    @Test
    void reactivar_registraElCambioDeEstado() {
        Docente docente = docente(2L);
        docente.setActivo(false);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(docente));
        when(usuarioRepository.save(docente)).thenReturn(docente);

        service.reactivar(2L);

        RegistrarAuditoriaRequestDTO auditoria = auditoriaRegistrada();
        assertEquals(TipoAccionAuditoria.CAMBIO_ESTADO, auditoria.getAccion());
        assertEquals("Reactivación de usuario", auditoria.getDescripcion());
        assertEquals("Inactivo", auditoria.getDetalles().get(0).getValorAnterior());
        assertEquals("Activo", auditoria.getDetalles().get(0).getValorNuevo());
    }

    @Test
    void desactivar_siLaAuditoriaFallaElErrorSePropagaParaRevertirElCambio() {
        Docente docente = docente(2L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(docente));
        when(seccionRepository.findByTutorId(2L)).thenReturn(List.of());
        when(usuarioRepository.save(docente)).thenReturn(docente);
        doThrow(new IllegalStateException("BD caída")).when(auditoriaService).registrarAuditoria(any());

        assertThrows(IllegalStateException.class, () -> service.desactivar(2L));
    }
}
