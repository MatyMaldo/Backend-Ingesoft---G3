package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.CursoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.*;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AsignacionCursoServiceImpl implements AsignacionCursoService {

    private final AsignacionCursoRepository asignacionRepo;
    private final CursoRepository cursoRepo;
    private final DocenteRepository docenteRepo;
    private final SeccionRepository seccionRepo;
    private final PeriodoMatriculaRepository periodoRepo;
    private final AuditoriaService auditoriaService;

    public AsignacionCursoServiceImpl(AsignacionCursoRepository asignacionRepo,
                                      CursoRepository cursoRepo,
                                      DocenteRepository docenteRepo,
                                      SeccionRepository seccionRepo,
                                      PeriodoMatriculaRepository periodoRepo,
                                      AuditoriaService auditoriaService) {
        this.asignacionRepo = asignacionRepo;
        this.cursoRepo = cursoRepo;
        this.docenteRepo = docenteRepo;
        this.seccionRepo = seccionRepo;
        this.periodoRepo = periodoRepo;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public AsignacionCursoResponse crear(CrearAsignacionCursoRequest request) {
        Curso curso = cursoRepo.findById(request.getCursoId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("curso", request.getCursoId()));
        Docente docente = docenteRepo.findById(request.getDocenteId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("docente", request.getDocenteId()));
        if (!docente.isActivo()) {
            throw new ReglaNegocioException("El docente no está activo.");
        }
        Seccion seccion = seccionRepo.findById(request.getSeccionId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("sección", request.getSeccionId()));

        if (!esProgramacionAbierta(seccion.getAnioAcademico().getId())) {
            throw new ReglaNegocioException("La programación de horarios está cerrada: solo se habilita mientras el año académico está en preparación.");
        }

        if (asignacionRepo.existsByCursoIdAndSeccionId(request.getCursoId(), request.getSeccionId())) {
            throw new ConflictoException("El curso ya está asignado a otro docente en esta sección.");
        }

        AsignacionCurso asignacion = new AsignacionCurso();
        asignacion.setCurso(curso);
        asignacion.setDocente(docente);
        asignacion.setSeccion(seccion);
        AsignacionCurso guardada = asignacionRepo.save(asignacion);

        auditoriaService.registrarAuditoria(new com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO(
                null, com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria.HORARIOS,
                com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria.CREAR,
                "AsignacionCurso", guardada.getId(),
                "Asignación de curso a sección", List.of()
        ));

        return mapear(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionCursoResponse> listar(Long gradoId, Long seccionId, Long anioId, Long docenteId) {
        List<AsignacionCurso> lista;
        if (seccionId != null) {
            lista = asignacionRepo.findBySeccionId(seccionId);
        } else if (gradoId != null && anioId != null) {
            lista = asignacionRepo.findBySeccionGradoIdAndSeccionAnioAcademicoId(gradoId, anioId);
        } else if (docenteId != null) {
            lista = asignacionRepo.findByDocenteId(docenteId);
        } else {
            lista = asignacionRepo.findAll();
        }
        return lista.stream().map(this::mapear).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AsignacionCursoResponse obtener(Long id) {
        AsignacionCurso a = asignacionRepo.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("asignación", id));
        return mapear(a);
    }

    @Override
    @Transactional
    public AsignacionCursoResponse actualizar(Long id, ActualizarAsignacionCursoRequest request) {
        AsignacionCurso asignacion = asignacionRepo.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("asignación", id));

        if (!esProgramacionAbierta(asignacion.getSeccion().getAnioAcademico().getId())) {
            throw new ReglaNegocioException("La programación de horarios está cerrada: solo se habilita mientras el año académico está en preparación.");
        }

        boolean cambioCurso = !asignacion.getCurso().getId().equals(request.getCursoId());
        boolean cambioDocente = !asignacion.getDocente().getId().equals(request.getDocenteId());

        if (!cambioCurso && !cambioDocente) {
            return mapear(asignacion);
        }

        if (cambioCurso) {
            if (asignacionRepo.existsByCursoIdAndSeccionId(request.getCursoId(), asignacion.getSeccion().getId())) {
                throw new ConflictoException("El curso ya está asignado a otro docente en esta sección.");
            }
            Curso nuevoCurso = cursoRepo.findById(request.getCursoId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("curso", request.getCursoId()));
            asignacion.setCurso(nuevoCurso);
        }

        if (cambioDocente) {
            Docente nuevoDocente = docenteRepo.findById(request.getDocenteId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("docente", request.getDocenteId()));
            if (!nuevoDocente.isActivo()) {
                throw new ReglaNegocioException("El docente no está activo.");
            }
            asignacion.setDocente(nuevoDocente);
        }

        AsignacionCurso guardada = asignacionRepo.save(asignacion);

        auditoriaService.registrarAuditoria(new com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO(
                null, com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria.HORARIOS,
                com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria.ACTUALIZAR,
                "AsignacionCurso", guardada.getId(),
                "Actualización de asignación", List.of()
        ));

        return mapear(guardada);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        AsignacionCurso asignacion = asignacionRepo.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("asignación", id));

        if (!esProgramacionAbierta(asignacion.getSeccion().getAnioAcademico().getId())) {
            throw new ReglaNegocioException("La programación de horarios está cerrada: solo se habilita mientras el año académico está en preparación.");
        }
        // TODO: cuando exista BloqueHorario, verificar que no tenga bloques asociados
        // (o eliminar los bloques con confirmación desde el frontend).
        asignacionRepo.delete(asignacion);

        auditoriaService.registrarAuditoria(new com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO(
                null, com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria.HORARIOS,
                com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria.ELIMINAR,
                "AsignacionCurso", id, "Eliminación de asignación", List.of()
        ));
    }

    private boolean esProgramacionAbierta(Long anioAcademicoId) {
        var periodo = periodoRepo.findByAnioAcademicoId(anioAcademicoId);
        if (periodo.isPresent()) {
            return !java.time.LocalDate.now().isAfter(periodo.get().getFechaFin());
        }
        return false;
    }

    private AsignacionCursoResponse mapear(AsignacionCurso a) {
        AsignacionCursoResponse r = new AsignacionCursoResponse();
        r.setId(a.getId());
        r.setCursoId(a.getCurso().getId());
        r.setCursoNombre(a.getCurso().getNombre());
        r.setDocenteId(a.getDocente().getId());
        r.setDocenteNombre(a.getDocente().getNombre() + " " + a.getDocente().getApellidoPaterno());
        r.setSeccionId(a.getSeccion().getId());
        r.setSeccionNombre(a.getSeccion().getNombre());
        r.setGradoId(a.getSeccion().getGrado().getId());
        r.setGradoNombre(a.getSeccion().getGrado().getNombre());
        r.setAnio(a.getSeccion().getAnioAcademico().getAnio());
        return r;
    }
}