package com.educoders.Gestion_Asistencia.servicio;

import com.educoders.Gestion_Asistencia.dominio.AsistenciaDocente;
import com.educoders.Gestion_Asistencia.dominio.AsistenciaEstudiante;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaDocenteRepository;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaEstudianteRepository;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaHistorialResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaSesionDetalleResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.GuardarAsistenciaEstudiantesRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Gestion_Matriz_Horarios.persistencia.SesionClaseRepository;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaServiceImplTest {

    @Mock private SesionClaseRepository sesionRepository;
    @Mock private AsistenciaEstudianteRepository asistenciaEstudianteRepository;
    @Mock private AsistenciaDocenteRepository asistenciaDocenteRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private Clock clock;

    @InjectMocks private AsistenciaServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void configurarReloj() {
        lenient().when(clock.instant()).thenReturn(Instant.now());
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Docente docente() {
        Docente docente = new Docente();
        docente.setId(2L);
        docente.setNombre("Juan");
        docente.setApellidoPaterno("Perez");
        return docente;
    }

    private Estudiante estudiante() {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(10L);
        estudiante.setCodigo("EST-001");
        estudiante.setNombre("Luis");
        estudiante.setApellidoPaterno("Ramirez");
        return estudiante;
    }

    private SesionClase sesion(LocalDateTime inicio, LocalDateTime fin,
                               EstadoSesion estado, Docente docente, Estudiante estudiante) {
        Grado grado = new Grado();
        grado.setId(1L);
        grado.setNombre("1er Grado");

        Seccion seccion = new Seccion();
        seccion.setId(3L);
        seccion.setNombre("A");
        seccion.setGrado(grado);

        Curso curso = new Curso();
        curso.setId(4L);
        curso.setNombre("Matematicas");

        AsignacionCurso asignacion = new AsignacionCurso();
        asignacion.setId(5L);
        asignacion.setCurso(curso);
        asignacion.setSeccion(seccion);
        asignacion.setDocente(docente);

        BloqueHorario bloque = new BloqueHorario();
        bloque.setId(6L);
        bloque.setDiaSemana(DiaSemana.LUNES);
        bloque.setHoraInicio(inicio.toLocalTime());
        bloque.setHoraFin(fin.toLocalTime());
        bloque.setAsignacionCurso(asignacion);

        SesionClase sesion = new SesionClase();
        sesion.setId(1L);
        sesion.setFecha(inicio.toLocalDate());
        sesion.setHoraInicio(inicio);
        sesion.setHoraFin(fin);
        sesion.setEstado(estado);
        sesion.setBloqueHorario(bloque);
        return sesion;
    }

    private Matricula matricula(Estudiante estudiante) {
        Matricula matricula = new Matricula();
        matricula.setId(20L);
        matricula.setEstudiante(estudiante);
        matricula.setEstado(EstadoMatricula.CONFIRMADA);
        return matricula;
    }

    private void autenticar(Docente docente) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(docente, null, List.of()));
    }

    private void autenticar(Estudiante estudiante) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(estudiante, null, List.of()));
    }

    private void autenticar(Apoderado apoderado) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(apoderado, null, List.of()));
    }

    private void stubsDetalle(Estudiante estudiante) {
        when(matriculaRepository.buscarPorSeccionYEstadoConEstudiante(
                3L, EstadoMatricula.CONFIRMADA))
                .thenReturn(List.of(matricula(estudiante)));
        when(asistenciaEstudianteRepository.findBySesionClaseId(1L))
                .thenReturn(List.of());
        when(asistenciaDocenteRepository.findBySesionClaseId(1L))
                .thenReturn(Optional.empty());
    }

    @Test
    void detalle_devuelveRosterYPermisos() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(5);
        LocalDateTime fin = LocalDateTime.now().plusMinutes(30);
        SesionClase sesion = sesion(inicio, fin, EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        stubsDetalle(estudiante);

        AsistenciaSesionDetalleResponse r = service.obtenerDetalle(1L);

        assertEquals(1, r.estudiantes().size());
        assertEquals(10L, r.estudiantes().get(0).estudianteId());
        assertTrue(r.puedeRegistrarDocente());
        assertTrue(r.puedeRegistrarEstudiantes());
    }

    @Test
    void guardarEstudiantes_exigeRosterExacto() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        stubsDetalle(estudiante);

        GuardarAsistenciaEstudiantesRequest request =
                new GuardarAsistenciaEstudiantesRequest(List.of(
                        new GuardarAsistenciaEstudiantesRequest.AsistenciaItem(
                                10L, EstadoAsistencia.PRESENTE, "")));

        service.guardarAsistenciaEstudiantes(1L, request);

        ArgumentCaptor<List<AsistenciaEstudiante>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(asistenciaEstudianteRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(EstadoAsistencia.PRESENTE, captor.getValue().get(0).getEstado());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void guardarEstudiantes_rechazaFaltantes() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(matriculaRepository.buscarPorSeccionYEstadoConEstudiante(
                3L, EstadoMatricula.CONFIRMADA))
                .thenReturn(List.of(matricula(estudiante)));

        GuardarAsistenciaEstudiantesRequest request =
                new GuardarAsistenciaEstudiantesRequest(List.of());

        assertThrows(ReglaNegocioException.class,
                () -> service.guardarAsistenciaEstudiantes(1L, request));
        verify(asistenciaEstudianteRepository, never()).saveAll(any());
    }

    @Test
    void docenteDentroDeDiezMinutosQuedaPresente() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(2),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaDocenteRepository.findBySesionClaseId(1L)).thenReturn(Optional.empty());
        when(asistenciaDocenteRepository.save(any(AsistenciaDocente.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var r = service.registrarAsistenciaDocente(1L);

        assertEquals(EstadoAsistencia.PRESENTE, r.estado());
    }

    @Test
    void docenteDespuesDeDiezMinutosQuedaTardanza() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(20),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaDocenteRepository.findBySesionClaseId(1L)).thenReturn(Optional.empty());
        when(asistenciaDocenteRepository.save(any(AsistenciaDocente.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var r = service.registrarAsistenciaDocente(1L);

        assertEquals(EstadoAsistencia.TARDANZA, r.estado());
    }

    @Test
    void docenteFueraDeVentana_rechaza() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaDocenteRepository.findBySesionClaseId(1L)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class,
                () -> service.registrarAsistenciaDocente(1L));
    }

    @Test
    void docenteDeOtraSesion_rechaza() {
        Docente docente = docente();
        Docente otro = new Docente();
        otro.setId(99L);
        otro.setNombre("Otro");
        otro.setApellidoPaterno("Docente");
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA, otro, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerDetalle(1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void historialEstudiante_devuelveCursosYRegistros() {
        Estudiante estudiante = estudiante();
        autenticar(estudiante);

        Docente docente = docente();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1).plusHours(2),
                EstadoSesion.FINALIZADA, docente, estudiante);
        AsistenciaEstudiante asistencia = new AsistenciaEstudiante();
        asistencia.setId(100L);
        asistencia.setSesionClase(sesion);
        asistencia.setEstudiante(estudiante);
        asistencia.setEstado(EstadoAsistencia.PRESENTE);
        asistencia.setFechaHoraRegistro(sesion.getHoraInicio());

        when(asistenciaEstudianteRepository.buscarHistorial(
                eq(10L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(asistencia)));
        when(asistenciaEstudianteRepository.buscarCursosHistorial(10L, null, null))
                .thenReturn(List.<Object[]>of(new Object[]{4L, "Matematicas"}));
        when(asistenciaEstudianteRepository.contarPorEstado(
                10L, null, null, null))
                .thenReturn(List.<Object[]>of(
                        new Object[]{EstadoAsistencia.PRESENTE, 1L}));

        AsistenciaHistorialResponse r = service.historialEstudiante(
                null, null, null, 0, 10);

        assertEquals(1, r.cursos().size());
        assertEquals(1, r.registros().size());
        assertEquals("Matematicas", r.registros().get(0).cursoNombre());
        assertEquals(0, r.pagina());
        assertEquals(10, r.tamano());
        assertEquals(1, r.totalPresentes());
        assertEquals(0, r.totalTardanzas());
        assertEquals(0, r.totalFaltas());
        assertEquals(1, r.totalRegistros());
        assertEquals(100.0, r.porcentajeAsistencia());
    }

    @Test
    void historialCalculaTotalesGlobalesYPorcentaje() {
        Estudiante estudiante = estudiante();
        autenticar(estudiante);

        when(asistenciaEstudianteRepository.buscarHistorial(
                eq(10L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(), PageRequest.of(0, 10), 4));
        when(asistenciaEstudianteRepository.buscarCursosHistorial(10L, null, null))
                .thenReturn(List.of());
        when(asistenciaEstudianteRepository.contarPorEstado(10L, null, null, null))
                .thenReturn(List.of(
                        new Object[]{EstadoAsistencia.PRESENTE, 2L},
                        new Object[]{EstadoAsistencia.TARDANZA, 1L},
                        new Object[]{EstadoAsistencia.FALTA, 1L}));

        AsistenciaHistorialResponse r = service.historialEstudiante(
                null, null, null, 0, 10);

        assertEquals(2, r.totalPresentes());
        assertEquals(1, r.totalTardanzas());
        assertEquals(1, r.totalFaltas());
        assertEquals(0, r.totalJustificadas());
        assertEquals(4, r.totalRegistros());
        assertEquals(75.0, r.porcentajeAsistencia());
    }

    @Test
    void historialApoderado_validaEstudianteACargo() {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(4L);
        apoderado.setNombre("Carlos");
        apoderado.setApellidoPaterno("Ramirez");
        autenticar(apoderado);

        Estudiante estudiante = estudiante();
        estudiante.setApoderado(apoderado);
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));
        when(asistenciaEstudianteRepository.buscarHistorial(
                eq(10L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(asistenciaEstudianteRepository.buscarCursosHistorial(10L, null, null))
                .thenReturn(List.of());
        when(asistenciaEstudianteRepository.contarPorEstado(
                10L, null, null, null)).thenReturn(List.of());

        AsistenciaHistorialResponse r = service.historialApoderado(
                10L, null, null, null, 0, 10);

        assertTrue(r.registros().isEmpty());
        verify(asistenciaEstudianteRepository).buscarHistorial(
                eq(10L), isNull(), isNull(), isNull(), any(Pageable.class));
    }
}
