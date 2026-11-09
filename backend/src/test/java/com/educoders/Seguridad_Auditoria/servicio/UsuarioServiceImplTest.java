package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.BimestreRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SeccionRepository seccionRepository;
    @Mock private BimestreRepository bimestreRepository;

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
}
