package com.educoders.Gestion_Asistencia.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.*;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.*;
import com.educoders.Gestion_Asistencia.dominio.AsistenciaEstudiante;
import com.educoders.Gestion_Asistencia.dominio.ReporteAsistencia;
import com.educoders.Gestion_Asistencia.persistencia.*;
import com.educoders.Gestion_Asistencia.presentacion.dto.GenerarReporteAsistenciaRequest;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.*;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReporteAsistenciaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 12);

    private final AsignacionCursoRepository asignaciones = mock(AsignacionCursoRepository.class);
    private final AsistenciaEstudianteRepository asistencias = mock(AsistenciaEstudianteRepository.class);
    private final MatriculaRepository matriculas = mock(MatriculaRepository.class);
    private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
    private final AnioAcademicoRepository anios = mock(AnioAcademicoRepository.class);
    private final ReporteAsistenciaRepository reportes = mock(ReporteAsistenciaRepository.class);
    private final ReporteAsistenciaService servicio = new ReporteAsistenciaService(
            asignaciones, asistencias, matriculas, estudiantes, anios, reportes,
            Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneOffset.UTC));

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cadaEmisionConservaSuPropiaCaptura() {
        prepararDocente();
        var asistencia = mock(AsistenciaEstudiante.class);
        var sesion = mock(SesionClase.class);
        when(asistencia.getSesionClase()).thenReturn(sesion);
        when(sesion.getFecha()).thenReturn(HOY);
        when(sesion.getHoraInicio()).thenReturn(HOY.atTime(8, 0));
        when(asistencia.getEstado()).thenReturn(EstadoAsistencia.FALTA, EstadoAsistencia.TARDANZA);
        when(asistencias.buscarFaltasParaReporte(11L, 21L, HOY, HOY,
                List.of(EstadoAsistencia.FALTA, EstadoAsistencia.TARDANZA))).thenReturn(List.of(asistencia));
        when(reportes.save(any(ReporteAsistencia.class))).thenAnswer(i -> i.getArgument(0));
        var request = new GenerarReporteAsistenciaRequest(HOY, HOY, "Seguimiento");

        servicio.generar(11L, 21L, request);
        servicio.generar(11L, 21L, request);

        var captor = ArgumentCaptor.forClass(ReporteAsistencia.class);
        verify(reportes, times(2)).save(captor.capture());
        var primero = captor.getAllValues().get(0);
        var segundo = captor.getAllValues().get(1);
        assertNotSame(primero, segundo);
        assertEquals(EstadoAsistencia.FALTA, primero.getFaltas().get(0).getEstado());
        assertEquals(EstadoAsistencia.TARDANZA, segundo.getFaltas().get(0).getEstado());
    }

    @Test
    void elRangoSinFaltasBloqueaLaEmision() {
        prepararDocente();

        assertFalse(servicio.vistaPrevia(11L, 21L, HOY, HOY).puedeGenerar());
        assertThrows(ReglaNegocioException.class, () -> servicio.generar(11L, 21L,
                new GenerarReporteAsistenciaRequest(HOY, HOY, "Seguimiento")));
        verifyNoInteractions(reportes);
    }

    @Test
    void elEstudianteSinMatriculaConfirmadaNoPuedeLeerElDetalle() {
        var estudiante = mock(Estudiante.class);
        when(estudiante.getId()).thenReturn(21L);
        autenticar(estudiante);
        var anio = mock(AnioAcademico.class);
        when(anio.getId()).thenReturn(1L);
        when(anios.findFirstByFechaInicioLessThanEqualOrderByFechaInicioDescIdDesc(HOY))
                .thenReturn(Optional.of(anio));

        assertTrue(servicio.listarPropios().reportes().isEmpty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.detallePropio(31L));
        verifyNoInteractions(reportes);
    }

    @Test
    void elApoderadoNoPuedeConsultarAUnEstudianteAjeno() {
        var apoderado = mock(Apoderado.class);
        autenticar(apoderado);
        var estudiante = mock(Estudiante.class);
        when(estudiantes.findById(21L)).thenReturn(Optional.of(estudiante));

        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> servicio.detalleDeEstudiante(21L, 31L)).getStatusCode().value());
        verifyNoInteractions(reportes);
    }

    private void prepararDocente() {
        var docente = mock(Docente.class);
        when(docente.getId()).thenReturn(1L);
        autenticar(docente);
        var asignacion = mock(AsignacionCurso.class, RETURNS_DEEP_STUBS);
        when(asignacion.getDocente()).thenReturn(docente);
        when(asignacion.getSeccion().getId()).thenReturn(3L);
        when(asignacion.getSeccion().getAnioAcademico().getId()).thenReturn(4L);
        when(asignaciones.findById(11L)).thenReturn(Optional.of(asignacion));
        var matricula = mock(Matricula.class);
        when(matricula.getEstado()).thenReturn(EstadoMatricula.CONFIRMADA);
        when(matricula.getEstudiante()).thenReturn(mock(Estudiante.class));
        when(matriculas.findByEstudianteIdAndSeccionId(21L, 3L)).thenReturn(Optional.of(matricula));
    }

    private void autenticar(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }
}
