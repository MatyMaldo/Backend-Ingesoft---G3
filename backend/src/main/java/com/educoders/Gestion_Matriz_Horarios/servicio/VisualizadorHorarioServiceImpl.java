package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.BloqueHorarioResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.HorarioSemanalResponse;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VisualizadorHorarioServiceImpl implements VisualizadorHorarioService {
    private final AnioAcademicoRepository anioRepo;
    private final SeccionRepository seccionRepo;
    private final BloqueHorarioRepository bloqueRepo;
    private final MatriculaRepository matriculaRepo;
    private final EstudianteRepository estudianteRepo;

    public VisualizadorHorarioServiceImpl(AnioAcademicoRepository anioRepo,
                                          SeccionRepository seccionRepo,
                                          BloqueHorarioRepository bloqueRepo,
                                          MatriculaRepository matriculaRepo,
                                          EstudianteRepository estudianteRepo) {
        this.anioRepo = anioRepo;
        this.seccionRepo = seccionRepo;
        this.bloqueRepo = bloqueRepo;
        this.matriculaRepo = matriculaRepo;
        this.estudianteRepo = estudianteRepo;
    }

    @Override
    public HorarioSemanalResponse porSeccion(Long seccionId) {
        exigirRol(Administrador.class);
        AnioAcademico activo = anioActivo();
        Seccion seccion = seccionRepo.findById(seccionId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("sección", seccionId));
        if (seccion.getAnioAcademico() == null
                || !activo.getId().equals(seccion.getAnioAcademico().getId())) {
            throw new ReglaNegocioException("La sección no pertenece al año académico activo.");
        }
        return respuesta(bloqueRepo.findByAsignacionCursoSeccionId(seccionId),
                "Aún no se ha registrado un horario para esta sección");
    }

    @Override
    public HorarioSemanalResponse porDocente(Long docenteId) {
        exigirRol(Administrador.class);
        AnioAcademico activo = anioActivo();
        // La consulta es intencionalmente válida aunque el docente esté inactivo.
        return respuesta(bloqueRepo.findByAsignacionCursoDocenteIdAndAsignacionCursoSeccionAnioAcademicoId(
                docenteId, activo.getId()), "No tiene carga horaria asignada");
    }

    @Override
    public HorarioSemanalResponse miHorarioDocente() {
        Docente docente = exigirRol(Docente.class);
        AnioAcademico activo = anioActivo();
        return respuesta(bloqueRepo.findByAsignacionCursoDocenteIdAndAsignacionCursoSeccionAnioAcademicoId(
                docente.getId(), activo.getId()), "Aún no se le ha asignado un horario programado");
    }

    @Override
    public HorarioSemanalResponse miHorarioEstudiante() {
        Estudiante estudiante = exigirRol(Estudiante.class);
        return horarioEstudiante(estudiante.getId(), anioActivo());
    }

    @Override
    public HorarioSemanalResponse porApoderado(Long estudianteId) {
        Apoderado apoderado = exigirRol(Apoderado.class);
        Estudiante estudiante = estudianteRepo.findById(estudianteId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("estudiante", estudianteId));
        if (estudiante.getApoderado() == null
                || !apoderado.getId().equals(estudiante.getApoderado().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El estudiante no se encuentra a su cargo.");
        }
        return horarioEstudiante(estudianteId, anioActivo());
    }

    private HorarioSemanalResponse horarioEstudiante(Long estudianteId, AnioAcademico activo) {
        Long seccionId = matriculaRepo.buscarMatriculasConSeccionYAnio(estudianteId).stream()
                .filter(m -> m.getEstado() == EstadoMatricula.CONFIRMADA)
                .map(Matricula::getSeccion)
                .filter(s -> s != null && s.getAnioAcademico() != null
                        && activo.getId().equals(s.getAnioAcademico().getId()))
                .map(Seccion::getId)
                .findFirst().orElse(null);
        if (seccionId == null) {
            return new HorarioSemanalResponse("SIN_MATRICULA",
                    "No tiene una matrícula confirmada en el año académico activo", List.of());
        }
        return respuesta(bloqueRepo.findByAsignacionCursoSeccionId(seccionId),
                "Aún no se le ha asignado un horario programado");
    }

    private AnioAcademico anioActivo() {
        LocalDate hoy = LocalDate.now();
        return anioRepo.findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(hoy, hoy)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No hay un año académico activo en la fecha actual."));
    }

    private <T extends Usuario> T exigirRol(Class<T> tipo) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        if (!tipo.isInstance(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para consultar este horario.");
        }
        return tipo.cast(usuario);
    }

    private HorarioSemanalResponse respuesta(List<BloqueHorario> bloques, String mensajeVacio) {
        List<BloqueHorarioResponse> datos = bloques.stream()
                .sorted(Comparator.comparing((BloqueHorario b) -> b.getDiaSemana().ordinal())
                        .thenComparing(BloqueHorario::getHoraInicio))
                .map(this::mapear)
                .toList();
        return new HorarioSemanalResponse(datos.isEmpty() ? "SIN_BLOQUES" : "OK",
                datos.isEmpty() ? mensajeVacio : "", datos);
    }

    private BloqueHorarioResponse mapear(BloqueHorario b) {
        var asignacion = b.getAsignacionCurso();
        var seccion = asignacion.getSeccion();
        BloqueHorarioResponse r = new BloqueHorarioResponse();
        r.setId(b.getId());
        r.setDiaSemana(b.getDiaSemana());
        r.setHoraInicio(b.getHoraInicio());
        r.setHoraFin(b.getHoraFin());
        r.setAulaId(b.getAula().getId());
        r.setAulaNombre(b.getAula().getNombre());
        r.setAsignacionCursoId(asignacion.getId());
        r.setCursoId(asignacion.getCurso().getId());
        r.setCursoNombre(asignacion.getCurso().getNombre());
        r.setDocenteId(asignacion.getDocente().getId());
        r.setDocenteNombre(asignacion.getDocente().getNombre() + " " + asignacion.getDocente().getApellidoPaterno());
        r.setSeccionId(seccion.getId());
        r.setSeccionNombre(seccion.getNombre());
        r.setGradoId(seccion.getGrado().getId());
        r.setGradoNombre(seccion.getGrado().getNombre());
        r.setAnio(seccion.getAnioAcademico().getAnio());
        return r;
    }
}
