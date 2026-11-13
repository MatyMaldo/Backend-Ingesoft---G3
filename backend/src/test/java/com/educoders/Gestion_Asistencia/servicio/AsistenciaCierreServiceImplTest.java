package com.educoders.Gestion_Asistencia.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Asistencia.dominio.AsistenciaDocente;
import com.educoders.Gestion_Asistencia.dominio.AsistenciaEstudiante;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaDocenteRepository;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaEstudianteRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Gestion_Matriz_Horarios.persistencia.SesionClaseRepository;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaCierreServiceImplTest {

    @Mock private SesionClaseRepository sesionRepository;
    @Mock private AsistenciaEstudianteRepository asistenciaEstudianteRepository;
    @Mock private AsistenciaDocenteRepository asistenciaDocenteRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private LogErrorService logErrorService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private Clock clock;

    @InjectMocks private AsistenciaCierreServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void configurarReloj() {
        lenient().when(clock.instant()).thenReturn(Instant.now());
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
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

    private SesionClase sesion(LocalDateTime inicio, LocalDateTime fin, EstadoSesion estado) {
        Docente docente = docente();
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
        asignacion.setCurso(curso);
        asignacion.setSeccion(seccion);
        asignacion.setDocente(docente);

        BloqueHorario bloque = new BloqueHorario();
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

    private Matricula matricula() {
        Matricula matricula = new Matricula();
        matricula.setId(20L);
        matricula.setEstudiante(estudiante());
        matricula.setEstado(EstadoMatricula.CONFIRMADA);
        return matricula;
    }

    @Test
    void sesionTerminada_seFinalizaYCreaFaltas() {
        SesionClase sesion = sesion(
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                EstadoSesion.PROGRAMADA);

        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(matriculaRepository.buscarPorSeccionYEstadoConEstudiante(
                3L, EstadoMatricula.CONFIRMADA)).thenReturn(List.of(matricula()));
        when(asistenciaEstudianteRepository.findBySesionClaseId(1L))
                .thenReturn(List.of());
        when(asistenciaDocenteRepository.findBySesionClaseId(1L))
                .thenReturn(Optional.empty());

        service.procesarSesion(1L);

        assertEquals(EstadoSesion.FINALIZADA, sesion.getEstado());
        ArgumentCaptor<List<AsistenciaEstudiante>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(asistenciaEstudianteRepository).saveAll(captor.capture());
        assertEquals(EstadoAsistencia.FALTA, captor.getValue().get(0).getEstado());
        verify(asistenciaDocenteRepository).save(any(AsistenciaDocente.class));
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void sesionEnCurso_seActivaSinCrearFaltas() {
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA);

        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));

        service.procesarSesion(1L);

        assertEquals(EstadoSesion.ACTIVA, sesion.getEstado());
        verify(asistenciaEstudianteRepository, never()).saveAll(any());
        verify(asistenciaDocenteRepository, never()).save(any());
    }

    @Test
    void sesionOmitida_seIgnora() {
        SesionClase sesion = sesion(
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                EstadoSesion.OMITIDA);

        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));

        service.procesarSesion(1L);

        verify(sesionRepository, never()).save(any());
        verify(asistenciaEstudianteRepository, never()).saveAll(any());
    }

    @Test
    void asistenciasExistentes_noSeDuplican() {
        SesionClase sesion = sesion(
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                EstadoSesion.PROGRAMADA);
        Matricula matricula = matricula();

        AsistenciaEstudiante existente = new AsistenciaEstudiante();
        existente.setSesionClase(sesion);
        existente.setEstudiante(matricula.getEstudiante());
        existente.setEstado(EstadoAsistencia.PRESENTE);
        existente.setFechaHoraRegistro(sesion.getHoraInicio());

        AsistenciaDocente docenteExistente = new AsistenciaDocente();
        docenteExistente.setSesionClase(sesion);
        docenteExistente.setDocente(sesion.getBloqueHorario()
                .getAsignacionCurso().getDocente());
        docenteExistente.setEstado(EstadoAsistencia.PRESENTE);
        docenteExistente.setFechaHoraRegistro(sesion.getHoraInicio());

        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(matriculaRepository.buscarPorSeccionYEstadoConEstudiante(
                3L, EstadoMatricula.CONFIRMADA)).thenReturn(List.of(matricula));
        when(asistenciaEstudianteRepository.findBySesionClaseId(1L))
                .thenReturn(List.of(existente));
        when(asistenciaDocenteRepository.findBySesionClaseId(1L))
                .thenReturn(Optional.of(docenteExistente));

        service.procesarSesion(1L);

        assertEquals(EstadoSesion.FINALIZADA, sesion.getEstado());
        verify(asistenciaEstudianteRepository, never()).saveAll(any());
        verify(asistenciaDocenteRepository, never()).save(any());
    }
}
