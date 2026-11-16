package com.educoders.Gestion_Asistencia.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Asistencia.dominio.AsistenciaEstudiante;
import com.educoders.Gestion_Asistencia.dominio.ReporteAsistencia;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaEstudianteRepository;
import com.educoders.Gestion_Asistencia.persistencia.ReporteAsistenciaRepository;
import com.educoders.Gestion_Asistencia.presentacion.controladores.ReporteAsistenciaController;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.exception.ManejadorGlobalExcepciones;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReporteAsistenciaConsultaHttpTest {

    private static final String BASE = "/api/v1/asistencias/reportes";
    private static final long ESTUDIANTE_ID = 5L;
    private static final long ANIO_ACTUAL = 2L;
    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    private final MatriculaRepository matriculas = mock(MatriculaRepository.class);
    private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
    private final AnioAcademicoRepository anios = mock(AnioAcademicoRepository.class);
    private final ReporteAsistenciaRepository reportes = mock(ReporteAsistenciaRepository.class);
    private final Estudiante estudiante = mock(Estudiante.class);
    private final Apoderado apoderado = mock(Apoderado.class);
    private final AtomicReference<EstadoMatricula> estado = new AtomicReference<>(EstadoMatricula.CONFIRMADA);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        when(estudiante.getId()).thenReturn(ESTUDIANTE_ID);
        when(estudiante.getApoderado()).thenReturn(apoderado);
        when(apoderado.getId()).thenReturn(7L);
        when(estudiantes.findById(ESTUDIANTE_ID)).thenReturn(Optional.of(estudiante));
        var anio = mock(AnioAcademico.class);
        when(anio.getId()).thenReturn(ANIO_ACTUAL);
        when(anios.findFirstByFechaInicioLessThanEqualOrderByFechaInicioDescIdDesc(HOY))
                .thenReturn(Optional.of(anio));
        when(matriculas.existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoIn(
                eq(ESTUDIANTE_ID), eq(ANIO_ACTUAL), anyCollection()))
                .thenAnswer(llamada -> llamada.<List<EstadoMatricula>>getArgument(2).contains(estado.get()));

        var actual = reporte(11L, ANIO_ACTUAL, HOY);
        var anterior = reporte(12L, 1L, HOY.minusYears(1));
        var futuro = reporte(13L, 3L, HOY.plusYears(1));
        when(reportes.findByEstudianteIdOrderByFechaEmisionDescIdDesc(ESTUDIANTE_ID))
                .thenReturn(List.of(futuro, actual, anterior));
        for (var reporte : List.of(actual, anterior, futuro)) {
            when(reportes.findByIdAndEstudianteId(reporte.getId(), ESTUDIANTE_ID))
                    .thenReturn(Optional.of(reporte));
        }

        var consulta = new ReporteAsistenciaService(mock(AsignacionCursoRepository.class),
                mock(AsistenciaEstudianteRepository.class), matriculas, estudiantes, anios, reportes,
                Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC));
        mvc = MockMvcBuilders.standaloneSetup(new ReporteAsistenciaController(consulta))
                .setControllerAdvice(new ManejadorGlobalExcepciones(mock(LogErrorService.class)))
                .build();
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoMatricula.class, names = {"PENDIENTE", "CANCELADA"})
    void ocultaUnReporteExistenteSiLaMatriculaNoEstaConfirmada(EstadoMatricula nuevoEstado) throws Exception {
        for (boolean comoApoderado : List.of(false, true)) {
            String ruta = autenticar(comoApoderado);
            estado.set(EstadoMatricula.CONFIRMADA);
            mvc.perform(get(ruta + "/11")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.datos.id").value(11));

            estado.set(nuevoEstado);
            mvc.perform(get(ruta)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.datos.reportes").isEmpty());
            mvc.perform(get(ruta + "/11")).andExpect(status().isNotFound());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void limitaLaConsultaAlAnioActual(boolean comoApoderado) throws Exception {
        String ruta = autenticar(comoApoderado);
        mvc.perform(get(ruta)).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.reportes.length()").value(1))
                .andExpect(jsonPath("$.datos.reportes[0].id").value(11));
        mvc.perform(get(ruta + "/12")).andExpect(status().isNotFound());
        mvc.perform(get(ruta + "/13")).andExpect(status().isNotFound());
    }

    private String autenticar(boolean comoApoderado) {
        Usuario usuario = comoApoderado ? apoderado : estudiante;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
        return BASE + (comoApoderado ? "/apoderado/" + ESTUDIANTE_ID : "/estudiante");
    }

    private ReporteAsistencia reporte(long id, long anio, LocalDate fecha) {
        var sesion = mock(SesionClase.class, RETURNS_DEEP_STUBS);
        when(sesion.getFecha()).thenReturn(fecha);
        when(sesion.getHoraInicio()).thenReturn(fecha.atTime(8, 0));
        var asignacion = sesion.getBloqueHorario().getAsignacionCurso();
        when(asignacion.getSeccion().getAnioAcademico().getId()).thenReturn(anio);
        when(asignacion.getCurso().getNombre()).thenReturn("Matemáticas");
        var asistencia = new AsistenciaEstudiante();
        asistencia.setSesionClase(sesion);
        asistencia.setEstado(EstadoAsistencia.FALTA);
        var reporte = new ReporteAsistencia(estudiante, mock(Docente.class),
                fecha, fecha, fecha, "Prueba HTTP", List.of(asistencia));
        ReflectionTestUtils.setField(reporte, "id", id);
        return reporte;
    }
}
