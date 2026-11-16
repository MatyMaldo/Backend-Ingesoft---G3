package com.educoders.Gestion_Asistencia.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Asistencia.dominio.AsistenciaEstudiante;
import com.educoders.Gestion_Asistencia.dominio.ReporteAsistencia;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaEstudianteRepository;
import com.educoders.Gestion_Asistencia.persistencia.ReporteAsistenciaRepository;
import com.educoders.Gestion_Asistencia.presentacion.dto.GenerarReporteAsistenciaRequest;
import com.educoders.Gestion_Asistencia.presentacion.dto.ReporteAsistenciaResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.ReporteAsistenciaResponse.*;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.*;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ReporteAsistenciaService {

    private static final String SIN_FALTAS =
            "No se puede generar reporte de un estudiante sin inasistencias ni tardanzas registradas";
    private static final String SIN_REPORTES =
            "No hay reportes de inasistencias y tardanzas generados para el estudiante";

    private final AsignacionCursoRepository asignaciones;
    private final AsistenciaEstudianteRepository asistencias;
    private final MatriculaRepository matriculas;
    private final EstudianteRepository estudiantes;
    private final AnioAcademicoRepository anios;
    private final ReporteAsistenciaRepository reportes;
    private final Clock clock;

    public ReporteAsistenciaService(AsignacionCursoRepository asignaciones,
                                    AsistenciaEstudianteRepository asistencias,
                                    MatriculaRepository matriculas, EstudianteRepository estudiantes,
                                    AnioAcademicoRepository anios, ReporteAsistenciaRepository reportes,
                                    Clock clock) {
        this.asignaciones = asignaciones;
        this.asistencias = asistencias;
        this.matriculas = matriculas;
        this.estudiantes = estudiantes;
        this.anios = anios;
        this.reportes = reportes;
        this.clock = clock;
    }

    public VistaPrevia vistaPrevia(Long asignacionId, Long estudianteId, LocalDate desde, LocalDate hasta) {
        var contexto = contextoDocente(asignacionId, estudianteId, desde, hasta);
        List<Falta> faltas = contexto.asistencias().stream()
                .map(a -> new Falta(a.getSesionClase().getFecha(),
                        a.getSesionClase().getHoraInicio().toLocalTime(), a.getEstado(), a.getObservaciones()))
                .toList();
        return new VistaPrevia(faltas, !faltas.isEmpty(),
                contexto.asistencias().isEmpty() ? SIN_FALTAS : null);
    }

    @Transactional
    public ReporteAsistenciaResponse generar(Long asignacionId, Long estudianteId,
                                             GenerarReporteAsistenciaRequest request) {
        var contexto = contextoDocente(asignacionId, estudianteId, request.desde(), request.hasta());
        if (contexto.asistencias().isEmpty()) {
            throw new ReglaNegocioException(SIN_FALTAS);
        }
        var reporte = new ReporteAsistencia(contexto.estudiante(), contexto.asignacion().getDocente(),
                LocalDate.now(clock), request.desde(), request.hasta(),
                request.comentarios().strip(), contexto.asistencias());
        return detalle(reportes.save(reporte), contexto.asignacion());
    }

    public Listado listarPropios() {
        return listar(exigirUsuario(Estudiante.class).getId(), SIN_REPORTES);
    }

    public ReporteAsistenciaResponse detallePropio(Long reporteId) {
        return buscarDetalle(exigirUsuario(Estudiante.class).getId(), reporteId);
    }

    public Listado listarDeEstudiante(Long estudianteId) {
        validarApoderado(estudianteId);
        return listar(estudianteId, SIN_REPORTES + " seleccionado");
    }

    public ReporteAsistenciaResponse detalleDeEstudiante(Long estudianteId, Long reporteId) {
        validarApoderado(estudianteId);
        return buscarDetalle(estudianteId, reporteId);
    }

    private ContextoDocente contextoDocente(Long asignacionId, Long estudianteId,
                                            LocalDate desde, LocalDate hasta) {
        var docente = exigirUsuario(Docente.class);
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        var asignacion = asignaciones.findById(asignacionId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("asignación de curso", asignacionId));
        if (!asignacion.getDocente().getId().equals(docente.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No dicta esta asignación de curso.");
        }

        var matricula = matriculas.findByEstudianteIdAndSeccionId(estudianteId, asignacion.getSeccion().getId())
                .filter(m -> m.getEstado() == EstadoMatricula.CONFIRMADA)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El estudiante requiere matrícula confirmada en esta sección."));
        List<AsistenciaEstudiante> registros = asistencias.buscarFaltasParaReporte(
                asignacionId, estudianteId, desde, hasta,
                List.of(EstadoAsistencia.FALTA, EstadoAsistencia.TARDANZA));
        return new ContextoDocente(asignacion, matricula.getEstudiante(), registros);
    }

    private record ContextoDocente(AsignacionCurso asignacion, Estudiante estudiante,
                                   List<AsistenciaEstudiante> asistencias) {
    }

    private Listado listar(Long estudianteId, String mensajeVacio) {
        List<Resumen> lista = anioDeConsulta(estudianteId)
                .map(anioId -> reportes.findByEstudianteIdOrderByFechaEmisionDescIdDesc(estudianteId)
                        .stream()
                        .flatMap(reporte -> asignacionDelReporte(reporte, anioId)
                                .map(asignacion -> resumen(reporte, asignacion)).stream())
                        .toList())
                .orElseGet(List::of);
        return new Listado(lista, lista.isEmpty() ? mensajeVacio : null);
    }

    private ReporteAsistenciaResponse buscarDetalle(Long estudianteId, Long reporteId) {
        return anioDeConsulta(estudianteId)
                .flatMap(anioId -> reportes.findByIdAndEstudianteId(reporteId, estudianteId)
                        .flatMap(reporte -> asignacionDelReporte(reporte, anioId)
                                .map(asignacion -> detalle(reporte, asignacion))))
                .orElseThrow(() -> RecursoNoEncontradoException.de("reporte de asistencia", reporteId));
    }

    private Optional<AsignacionCurso> asignacionDelReporte(ReporteAsistencia reporte, Long anioId) {
        // Los detalles históricos son la fuente del contexto académico de la emisión.
        var asignacionesDelReporte = reporte.getFaltas().stream()
                .map(falta -> falta.getAsistencia().getSesionClase().getBloqueHorario().getAsignacionCurso())
                .distinct()
                .limit(2)
                .toList();
        if (asignacionesDelReporte.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(asignacionesDelReporte.get(0))
                .filter(asignacion -> anioId.equals(asignacion.getSeccion().getAnioAcademico().getId()));
    }

    private Optional<Long> anioDeConsulta(Long estudianteId) {
        return anios.findFirstByFechaInicioLessThanEqualOrderByFechaInicioDescIdDesc(LocalDate.now(clock))
                .map(AnioAcademico::getId)
                .filter(id -> matriculas.existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoIn(
                        estudianteId, id, List.of(EstadoMatricula.CONFIRMADA)));
    }

    private void validarApoderado(Long estudianteId) {
        var apoderado = exigirUsuario(Apoderado.class);
        var estudiante = estudiantes.findById(estudianteId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("estudiante", estudianteId));
        if (estudiante.getApoderado() == null
                || !estudiante.getApoderado().getId().equals(apoderado.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El estudiante no se encuentra a su cargo.");
        }
    }

    private <T extends Usuario> T exigirUsuario(Class<T> tipo) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        if (!tipo.isInstance(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario sin permisos para esta operación.");
        }
        return tipo.cast(usuario);
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() + " " + usuario.getApellidoPaterno();
        if (usuario.getApellidoMaterno() != null && !usuario.getApellidoMaterno().isBlank()) {
            nombre += " " + usuario.getApellidoMaterno();
        }
        return nombre;
    }

    private Resumen resumen(ReporteAsistencia reporte, AsignacionCurso asignacion) {
        var seccion = asignacion.getSeccion();
        return new Resumen(reporte.getId(), reporte.getFechaEmision(), reporte.getDesde(), reporte.getHasta(),
                nombreCompleto(reporte.getDocente()), seccion.getGrado().getNombre(),
                seccion.getNombre(), asignacion.getCurso().getNombre());
    }

    private ReporteAsistenciaResponse detalle(ReporteAsistencia reporte, AsignacionCurso asignacion) {
        var seccion = asignacion.getSeccion();
        List<Falta> faltas = reporte.getFaltas().stream()
                .map(falta -> new Falta(falta.getFecha(),
                        falta.getAsistencia().getSesionClase().getHoraInicio().toLocalTime(),
                        falta.getEstado(), falta.getObservacion()))
                .toList();
        return new ReporteAsistenciaResponse(reporte.getId(), reporte.getFechaEmision(),
                reporte.getDesde(), reporte.getHasta(), nombreCompleto(reporte.getDocente()),
                nombreCompleto(reporte.getEstudiante()), seccion.getGrado().getNombre(),
                seccion.getNombre(), asignacion.getCurso().getNombre(), reporte.getComentarios(), faltas);
    }
}
