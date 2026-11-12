package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.ApoderadoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.EstudianteApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarApoderadoRequest;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApoderadoServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ApoderadoRepository apoderadoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private ApoderadoServiceImpl service;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Apoderado crearApoderado(Long id) {
        Apoderado a = new Apoderado();
        a.setId(id);
        a.setNombre("Carlos");
        a.setApellidoPaterno("Ramirez");
        a.setApellidoMaterno("Torres");
        return a;
    }

    private Administrador crearAdmin() {
        Administrador a = new Administrador();
        a.setId(1L);
        return a;
    }

    private Estudiante crearEstudiante(Long id, String nombre, Apoderado apoderado) {
        Estudiante e = new Estudiante();
        e.setId(id);
        e.setNombre(nombre);
        e.setApellidoPaterno("Ramirez");
        e.setApellidoMaterno("Torres");
        e.setDni("DNI-" + id);
        e.setCodigo("EST-" + id);
        e.setApoderado(apoderado);
        return e;
    }

    private void autenticar(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    @Test
    void listarMisEstudiantes_sinAnio_devuelveDatosBasicos() {
        Apoderado apoderado = crearApoderado(4L);
        autenticar(apoderado);

        Estudiante luis = crearEstudiante(5L, "Luis", apoderado);
        Estudiante ana = crearEstudiante(6L, "Ana", apoderado);
        when(estudianteRepository.findByApoderadoIdOrderByIdAsc(4L))
                .thenReturn(List.of(luis, ana));

        List<EstudianteApoderadoResponse> resultado = service.listarMisEstudiantes(null);

        assertEquals(2, resultado.size());
        assertEquals(5L, resultado.get(0).id());
        assertEquals("Luis Ramirez Torres", resultado.get(0).nombreCompleto());
        assertEquals("DNI-5", resultado.get(0).dni());
        assertEquals("EST-5", resultado.get(0).codigo());
        assertNull(resultado.get(0).gradoId());
        assertNull(resultado.get(0).seccionId());
        assertNull(resultado.get(0).estadoMatricula());
        verifyNoInteractions(matriculaRepository);
    }

    @Test
    void listarMisEstudiantes_conAnio_completaGradoYSeccion() {
        Apoderado apoderado = crearApoderado(4L);
        autenticar(apoderado);

        Estudiante luis = crearEstudiante(5L, "Luis", apoderado);
        Estudiante ana = crearEstudiante(6L, "Ana", apoderado);

        AnioAcademico anio = new AnioAcademico();
        anio.setId(2L);
        anio.setAnio(2026);

        Grado grado = new Grado();
        grado.setId(1L);
        grado.setNombre("1er Grado de Secundaria");

        Seccion seccion = new Seccion();
        seccion.setId(3L);
        seccion.setNombre("A");
        seccion.setGrado(grado);
        seccion.setAnioAcademico(anio);

        Matricula matricula = new Matricula();
        matricula.setId(10L);
        matricula.setEstado(EstadoMatricula.CONFIRMADA);
        matricula.setEstudiante(luis);
        matricula.setSeccion(seccion);

        when(estudianteRepository.findByApoderadoIdOrderByIdAsc(4L))
                .thenReturn(List.of(luis, ana));
        when(matriculaRepository.buscarPorEstudianteYAnioIdYEstados(
                5L, 2L, List.of(EstadoMatricula.PENDIENTE, EstadoMatricula.CONFIRMADA)))
                .thenReturn(List.of(matricula));
        when(matriculaRepository.buscarPorEstudianteYAnioIdYEstados(
                6L, 2L, List.of(EstadoMatricula.PENDIENTE, EstadoMatricula.CONFIRMADA)))
                .thenReturn(List.of());

        List<EstudianteApoderadoResponse> resultado = service.listarMisEstudiantes(2L);

        assertEquals(1L, resultado.get(0).gradoId());
        assertEquals("1er Grado de Secundaria", resultado.get(0).gradoNombre());
        assertEquals(3L, resultado.get(0).seccionId());
        assertEquals("A", resultado.get(0).seccionNombre());
        assertEquals(2L, resultado.get(0).anioAcademicoId());
        assertEquals(2026, resultado.get(0).anioAcademico());
        assertEquals(EstadoMatricula.CONFIRMADA, resultado.get(0).estadoMatricula());

        assertNull(resultado.get(1).gradoId());
        assertNull(resultado.get(1).estadoMatricula());
    }

    @Test
    void listarMisEstudiantes_rolNoApoderado_rechaza() {
        autenticar(crearAdmin());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.listarMisEstudiantes(null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(estudianteRepository, never()).findByApoderadoIdOrderByIdAsc(anyLong());
    }

    @Test
    void listarMisEstudiantes_sinAutenticacion_rechaza() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.listarMisEstudiantes(null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(estudianteRepository, never()).findByApoderadoIdOrderByIdAsc(anyLong());
    }

    @Test
    void registrarApoderado_creaLaCuentaSinContrasena() {
        when(usuarioRepository.existsByDniAndTipoUsuario(anyString(), any())).thenReturn(false);
        when(usuarioRepository.existsByEmail("rosa@example.com")).thenReturn(false);
        when(apoderadoRepository.save(any(Apoderado.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarApoderado(new RegistrarApoderadoRequest(
                "Rosa", "Apoderado", null, "11000001", "rosa@example.com", "910000001", 36));

        ArgumentCaptor<Apoderado> captor = ArgumentCaptor.forClass(Apoderado.class);
        verify(apoderadoRepository).save(captor.capture());
        assertNull(captor.getValue().getPasswordHash());
        assertEquals("rosa", captor.getValue().getUsername());
        assertEquals(true, captor.getValue().isActivo());
    }
}
