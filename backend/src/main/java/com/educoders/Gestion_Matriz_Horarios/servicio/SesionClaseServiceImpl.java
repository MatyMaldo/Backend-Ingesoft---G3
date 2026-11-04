package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.SesionClaseRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.*;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SesionClaseServiceImpl implements SesionClaseService {

    private final SesionClaseRepository repo;
    private final BloqueHorarioRepository bloqueRepo;
    private final PeriodoMatriculaRepository periodoRepo;
    private final MatriculaRepository matriculaRepo;
    private final EstudianteRepository estudianteRepo;

    public SesionClaseServiceImpl(SesionClaseRepository repo,
                                  BloqueHorarioRepository bloqueRepo,
                                  PeriodoMatriculaRepository periodoRepo,
                                  MatriculaRepository matriculaRepo,
                                  EstudianteRepository estudianteRepo) {
        this.repo = repo;
        this.bloqueRepo = bloqueRepo;
        this.periodoRepo = periodoRepo;
        this.matriculaRepo = matriculaRepo;
        this.estudianteRepo = estudianteRepo;
    }

    // ------------------------------------------------------------------
    // GENERACIÓN
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public GenerarSesionesResponse generarSesiones(GenerarSesionesRequest request) {
        validarFechas(request.getFechaInicio(), request.getFechaFin());
        validarPeriodoCerrado(request.getAnioAcademicoId());

        List<BloqueHorario> bloques = bloqueRepo.findByAsignacionCursoSeccionId(request.getSeccionId());

        Set<String> omitidas = new HashSet<>();
        for (SesionOmitida o : request.getSesionesOmitidas()) {
            omitidas.add(clave(o.getBloqueHorarioId(), o.getFecha()));
        }

        GenerarSesionesResponse resumen = new GenerarSesionesResponse();
        resumen.setTotalBloquesProcesados(bloques.size());

        LocalDate fecha = request.getFechaInicio();
        while (!fecha.isAfter(request.getFechaFin())) {
            DiaSemana dia = toDiaSemana(fecha.getDayOfWeek());
            if (dia != null) {
                for (BloqueHorario bloque : bloques) {
                    if (bloque.getDiaSemana() != dia) {
                        continue;
                    }
                    if (omitidas.contains(clave(bloque.getId(), fecha))) {
                        resumen.setSesionesOmitidas(resumen.getSesionesOmitidas() + 1);
                        continue;
                    }
                    if (repo.existsByBloqueHorarioIdAndFecha(bloque.getId(), fecha)) {
                        resumen.setSesionesYaExistentes(resumen.getSesionesYaExistentes() + 1);
                        continue;
                    }
                    try {
                        SesionClase sesion = new SesionClase();
                        sesion.setFecha(fecha);
                        sesion.setHoraInicio(LocalDateTime.of(fecha, bloque.getHoraInicio()));
                        sesion.setHoraFin(LocalDateTime.of(fecha, bloque.getHoraFin()));
                        sesion.setEstado(EstadoSesion.PROGRAMADA);
                        sesion.setBloqueHorario(bloque);
                        repo.save(sesion);
                        resumen.setSesionesCreadas(resumen.getSesionesCreadas() + 1);
                    } catch (Exception e) {
                        resumen.getErrores().add(
                                "Error en bloque " + bloque.getId() + " fecha " + fecha + ": " + e.getMessage());
                    }
                }
            }
            fecha = fecha.plusDays(1);
        }

        return resumen;
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewSesionesResponse previewSesiones(PreviewSesionesRequest request) {
        validarFechas(request.getFechaInicio(), request.getFechaFin());
        validarPeriodoCerrado(request.getAnioAcademicoId());

        List<BloqueHorario> bloques = bloqueRepo.findByAsignacionCursoSeccionId(request.getSeccionId());

        PreviewSesionesResponse respuesta = new PreviewSesionesResponse();

        LocalDate fecha = request.getFechaInicio();
        while (!fecha.isAfter(request.getFechaFin())) {
            DiaSemana dia = toDiaSemana(fecha.getDayOfWeek());
            if (dia != null) {
                for (BloqueHorario bloque : bloques) {
                    if (bloque.getDiaSemana() != dia) {
                        continue;
                    }
                    SesionPreviewItem item = new SesionPreviewItem();
                    item.setBloqueHorarioId(bloque.getId());
                    item.setFecha(fecha);
                    item.setHoraInicio(bloque.getHoraInicio());
                    item.setHoraFin(bloque.getHoraFin());
                    item.setDiaSemana(dia);
                    item.setAulaId(bloque.getAula().getId());
                    item.setAulaNombre(bloque.getAula().getNombre());
                    item.setCursoId(bloque.getAsignacionCurso().getCurso().getId());
                    item.setCursoNombre(bloque.getAsignacionCurso().getCurso().getNombre());
                    item.setDocenteId(bloque.getAsignacionCurso().getDocente().getId());
                    item.setDocenteNombre(bloque.getAsignacionCurso().getDocente().getNombre()
                            + " " + bloque.getAsignacionCurso().getDocente().getApellidoPaterno());
                    item.setYaExiste(repo.existsByBloqueHorarioIdAndFecha(bloque.getId(), fecha));
                    respuesta.getSesiones().add(item);
                }
            }
            fecha = fecha.plusDays(1);
        }

        respuesta.setTotal(respuesta.getSesiones().size());
        return respuesta;
    }

    // ------------------------------------------------------------------
    // LECTURA
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<SesionClaseResponse> listarPorSeccion(Long seccionId, LocalDate fechaInicio, LocalDate fechaFin) {
        validarFechas(fechaInicio, fechaFin);

        return repo.findByBloqueHorarioAsignacionCursoSeccionIdAndFechaBetween(seccionId, fechaInicio, fechaFin)
                .stream().map(this::mapear).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionClaseResponse> listarPorDocente(Long docenteId, LocalDate fechaInicio, LocalDate fechaFin) {
        validarFechas(fechaInicio, fechaFin);

        return repo.findByBloqueHorarioAsignacionCursoDocenteIdAndFechaBetween(docenteId, fechaInicio, fechaFin)
                .stream().map(this::mapear).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionClaseResponse> listarMiHorarioDocente(LocalDate fechaInicio, LocalDate fechaFin) {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (!(usuario instanceof Docente docente)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un docente puede consultar su propio horario.");
        }
        return listarPorDocente(docente.getId(), fechaInicio, fechaFin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionClaseResponse> listarMiHorarioEstudiante(LocalDate fechaInicio, LocalDate fechaFin) {
        validarFechas(fechaInicio, fechaFin);

        Usuario usuario = obtenerUsuarioAutenticado();
        if (!(usuario instanceof Estudiante estudiante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un estudiante puede consultar su propio horario.");
        }

        Long seccionId = obtenerSeccionDelEstudiante(estudiante.getId(), fechaInicio.getYear());
        if (seccionId == null) {
            return List.of();
        }
        return listarPorSeccion(seccionId, fechaInicio, fechaFin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionClaseResponse> listarPorApoderado(Long estudianteId, LocalDate fechaInicio, LocalDate fechaFin) {
        validarFechas(fechaInicio, fechaFin);

        Usuario usuario = obtenerUsuarioAutenticado();
        if (!(usuario instanceof Apoderado apoderado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un apoderado puede consultar el horario de sus estudiantes.");
        }

        Estudiante estudiante = estudianteRepo.findById(estudianteId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("estudiante", estudianteId));

        if (estudiante.getApoderado() == null
                || !estudiante.getApoderado().getId().equals(apoderado.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El estudiante no se encuentra a su cargo.");
        }

        Long seccionId = obtenerSeccionDelEstudiante(estudianteId, fechaInicio.getYear());
        if (seccionId == null) {
            return List.of();
        }
        return listarPorSeccion(seccionId, fechaInicio, fechaFin);
    }

    @Override
    @Transactional(readOnly = true)
    public SesionClaseResponse obtener(Long id) {
        SesionClase s = repo.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("sesión de clase", id));
        return mapear(s);
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------

    private void validarFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new ReglaNegocioException("Las fechas de inicio y fin son obligatorias.");
        }
        if (fin.isBefore(inicio)) {
            throw new ReglaNegocioException("La fecha de fin debe ser posterior o igual a la de inicio.");
        }
    }

    private void validarPeriodoCerrado(Long anioAcademicoId) {
        PeriodoMatricula periodo = periodoRepo.findByAnioAcademicoId(anioAcademicoId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No existe un periodo de matrícula registrado para este año académico."));
        if (!LocalDate.now().isAfter(periodo.getFechaFin())) {
            throw new ReglaNegocioException(
                    "La generación de sesiones solo se habilita después de que termine el periodo de matrícula.");
        }
    }

    /** Convierte el día de Java (inglés) al enum del dominio (español). Sábado/domingo → null. */
    private DiaSemana toDiaSemana(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            default -> null;
        };
    }

    private String clave(Long bloqueId, LocalDate fecha) {
        return bloqueId + "|" + fecha;
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return usuario;
    }

    /** Devuelve la sección de la matrícula del estudiante para el año indicado, o null si no tiene. */
    private Long obtenerSeccionDelEstudiante(Long estudianteId, Integer anio) {
        List<Matricula> matriculas = matriculaRepo.buscarMatriculasConSeccionYAnio(estudianteId);
        return matriculas.stream()
                .filter(m -> m.getEstado() != EstadoMatricula.CANCELADA)
                .filter(m -> m.getSeccion() != null
                        && m.getSeccion().getAnioAcademico() != null
                        && anio.equals(m.getSeccion().getAnioAcademico().getAnio()))
                .map(m -> m.getSeccion().getId())
                .findFirst()
                .orElse(null);
    }

    private SesionClaseResponse mapear(SesionClase s) {
        SesionClaseResponse r = new SesionClaseResponse();
        r.setId(s.getId());
        r.setFecha(s.getFecha());
        r.setHoraInicio(s.getHoraInicio().toLocalTime());
        r.setHoraFin(s.getHoraFin().toLocalTime());
        r.setEstado(s.getEstado());
        r.setBloqueHorarioId(s.getBloqueHorario().getId());
        r.setDiaSemana(s.getBloqueHorario().getDiaSemana());
        r.setAulaId(s.getBloqueHorario().getAula().getId());
        r.setAulaNombre(s.getBloqueHorario().getAula().getNombre());
        r.setCursoId(s.getBloqueHorario().getAsignacionCurso().getCurso().getId());
        r.setCursoNombre(s.getBloqueHorario().getAsignacionCurso().getCurso().getNombre());
        r.setDocenteId(s.getBloqueHorario().getAsignacionCurso().getDocente().getId());
        r.setDocenteNombre(s.getBloqueHorario().getAsignacionCurso().getDocente().getNombre()
                + " " + s.getBloqueHorario().getAsignacionCurso().getDocente().getApellidoPaterno());
        r.setSeccionId(s.getBloqueHorario().getAsignacionCurso().getSeccion().getId());
        r.setSeccionNombre(s.getBloqueHorario().getAsignacionCurso().getSeccion().getNombre());
        return r;
    }
}