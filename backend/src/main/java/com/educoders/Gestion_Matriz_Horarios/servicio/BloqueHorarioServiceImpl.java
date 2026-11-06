package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.persistencia.AulaRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.*;
import com.educoders.Gestion_Matricula_Pagos.dominio.VentanaMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class BloqueHorarioServiceImpl implements BloqueHorarioService {

    private final BloqueHorarioRepository bloqueRepo;
    private final AsignacionCursoRepository asignacionRepo;
    private final AulaRepository aulaRepo;
    private final SeccionRepository seccionRepo;
    private final PeriodoMatriculaRepository periodoRepo;
    private final AuditoriaService auditoriaService;

    public BloqueHorarioServiceImpl(BloqueHorarioRepository bloqueRepo,
                                    AsignacionCursoRepository asignacionRepo,
                                    AulaRepository aulaRepo,
                                    SeccionRepository seccionRepo,
                                    PeriodoMatriculaRepository periodoRepo,
                                    AuditoriaService auditoriaService) {
        this.bloqueRepo = bloqueRepo;
        this.asignacionRepo = asignacionRepo;
        this.aulaRepo = aulaRepo;
        this.seccionRepo = seccionRepo;
        this.periodoRepo = periodoRepo;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public BloqueHorarioResponse crear(CrearBloqueRequest request) {
        AsignacionCurso asignacion = asignacionRepo.findById(request.getAsignacionCursoId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("asignación", request.getAsignacionCursoId()));
        Aula aula = aulaRepo.findById(request.getAulaId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("aula", request.getAulaId()));

        Seccion seccion = asignacion.getSeccion();
        Long anioId = seccion.getAnioAcademico().getId();

        if (!esProgramacionAbierta(anioId)) {
            throw new ReglaNegocioException("La programación de horarios está cerrada: solo se habilita durante el periodo de matrícula.");
        }

        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la de inicio.");
        }

        Long docenteId = asignacion.getDocente().getId();
        Long seccionId = seccion.getId();

        // Conflicto docente
        List<BloqueHorario> conflictoDocente = bloqueRepo.findConflictoDocente(
                request.getDiaSemana(), request.getHoraInicio(), request.getHoraFin(),
                anioId, null, docenteId);
        if (!conflictoDocente.isEmpty()) {
            throw new ConflictoException("No se puede asignar al docente seleccionado en 2 horarios simultáneos.");
        }

        // Conflicto sección
        List<BloqueHorario> conflictoSeccion = bloqueRepo.findConflictoSeccion(
                request.getDiaSemana(), request.getHoraInicio(), request.getHoraFin(),
                anioId, null, seccionId);
        if (!conflictoSeccion.isEmpty()) {
            throw new ConflictoException("No se puede asignar 2 cursos en el mismo bloque de horario.");
        }

        // Conflicto aula
        List<BloqueHorario> conflictoAula = bloqueRepo.findConflictoAula(
                request.getDiaSemana(), request.getHoraInicio(), request.getHoraFin(),
                anioId, null, request.getAulaId());
        if (!conflictoAula.isEmpty()) {
            throw new ConflictoException("El aula seleccionada ya está asignada en ese bloque de horario.");
        }

        BloqueHorario bloque = new BloqueHorario();
        bloque.setDiaSemana(request.getDiaSemana());
        bloque.setHoraInicio(request.getHoraInicio());
        bloque.setHoraFin(request.getHoraFin());
        bloque.setAula(aula);
        bloque.setAsignacionCurso(asignacion);
        BloqueHorario guardado = bloqueRepo.save(bloque);

        auditoriaService.registrarAuditoria(new com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO(
                null, com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria.HORARIOS,
                com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria.CREAR,
                "BloqueHorario", guardado.getId(),
                "Creación de bloque horario", List.of()
        ));

        return mapear(guardado);
    }

    @Override
    @Transactional
    public BloqueHorarioResponse actualizar(Long id, ActualizarBloqueRequest request) {
        BloqueHorario bloque = bloqueRepo.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("bloque horario", id));

        Seccion seccion = bloque.getAsignacionCurso().getSeccion();
        Long anioId = seccion.getAnioAcademico().getId();

        if (!esProgramacionAbierta(anioId)) {
            throw new ReglaNegocioException("La programación de horarios está cerrada: solo se habilita durante el periodo de matrícula.");
        }

        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la de inicio.");
        }

        // Si cambia asignación, validar nueva
        AsignacionCurso nuevaAsignacion = bloque.getAsignacionCurso();
        if (!bloque.getAsignacionCurso().getId().equals(request.getAsignacionCursoId())) {
            nuevaAsignacion = asignacionRepo.findById(request.getAsignacionCursoId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("asignación", request.getAsignacionCursoId()));
            Seccion nuevaSeccion = nuevaAsignacion.getSeccion();
            if (!nuevaSeccion.getAnioAcademico().getId().equals(anioId)) {
                throw new ReglaNegocioException("La asignación debe pertenecer al mismo año académico.");
            }
        }

        Aula nuevaAula = bloque.getAula();
        if (!bloque.getAula().getId().equals(request.getAulaId())) {
            nuevaAula = aulaRepo.findById(request.getAulaId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("aula", request.getAulaId()));
        }

        Long docenteId = nuevaAsignacion.getDocente().getId();
        Long seccionId = nuevaAsignacion.getSeccion().getId();
        Long aulaId = nuevaAula.getId();

        // Conflictos (excluyendo el propio bloque)
        List<BloqueHorario> cd = bloqueRepo.findConflictoDocente(
                request.getDiaSemana(), request.getHoraInicio(), request.getHoraFin(),
                anioId, id, docenteId);
        if (!cd.isEmpty()) {
            throw new ConflictoException("No se puede asignar al docente seleccionado en 2 horarios simultáneos.");
        }
        List<BloqueHorario> cs = bloqueRepo.findConflictoSeccion(
                request.getDiaSemana(), request.getHoraInicio(), request.getHoraFin(),
                anioId, id, seccionId);
        if (!cs.isEmpty()) {
            throw new ConflictoException("No se puede asignar 2 cursos en el mismo bloque de horario.");
        }
        List<BloqueHorario> ca = bloqueRepo.findConflictoAula(
                request.getDiaSemana(), request.getHoraInicio(), request.getHoraFin(),
                anioId, id, aulaId);
        if (!ca.isEmpty()) {
            throw new ConflictoException("El aula seleccionada ya está asignada en ese bloque de horario.");
        }

        bloque.setDiaSemana(request.getDiaSemana());
        bloque.setHoraInicio(request.getHoraInicio());
        bloque.setHoraFin(request.getHoraFin());
        bloque.setAula(nuevaAula);
        bloque.setAsignacionCurso(nuevaAsignacion);
        BloqueHorario guardado = bloqueRepo.save(bloque);

        auditoriaService.registrarAuditoria(new com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO(
                null, com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria.HORARIOS,
                com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria.ACTUALIZAR,
                "BloqueHorario", guardado.getId(),
                "Actualización de bloque horario", List.of()
        ));

        return mapear(guardado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        BloqueHorario bloque = bloqueRepo.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("bloque horario", id));

        Long anioId = bloque.getAsignacionCurso().getSeccion().getAnioAcademico().getId();
        if (!esProgramacionAbierta(anioId)) {
            throw new ReglaNegocioException("La programación de horarios está cerrada: solo se habilita durante el periodo de matrícula.");
        }

        bloqueRepo.delete(bloque);

        auditoriaService.registrarAuditoria(new com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO(
                null, com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria.HORARIOS,
                com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria.ELIMINAR,
                "BloqueHorario", id, "Eliminación de bloque horario", List.of()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueHorarioResponse> listar(Long gradoId, Long seccionId, Long anioId, Long docenteId) {
        List<BloqueHorario> lista;
        if (seccionId != null) {
            lista = bloqueRepo.findByAsignacionCursoSeccionId(seccionId);
        } else if (gradoId != null && anioId != null) {
            lista = bloqueRepo.findByAsignacionCursoSeccionGradoIdAndAsignacionCursoSeccionAnioAcademicoId(gradoId, anioId);
        } else if (docenteId != null && anioId != null) {
            lista = bloqueRepo.findByAsignacionCursoDocenteIdAndAsignacionCursoSeccionAnioAcademicoId(docenteId, anioId);
        } else {
            lista = bloqueRepo.findAll();
        }
        return lista.stream().map(this::mapear).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BloqueHorarioResponse obtener(Long id) {
        BloqueHorario b = bloqueRepo.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("bloque horario", id));
        return mapear(b);
    }

    private boolean esProgramacionAbierta(Long anioAcademicoId) {
        return periodoRepo.findByAnioAcademicoId(anioAcademicoId)
                .map(periodo -> VentanaMatricula.crudBloquesHorarioAbierta(
                        periodo, java.time.LocalDate.now()))
                .orElse(false);
    }

    private BloqueHorarioResponse mapear(BloqueHorario b) {
        BloqueHorarioResponse r = new BloqueHorarioResponse();
        r.setId(b.getId());
        r.setDiaSemana(b.getDiaSemana());
        r.setHoraInicio(b.getHoraInicio());
        r.setHoraFin(b.getHoraFin());
        r.setAulaId(b.getAula().getId());
        r.setAulaNombre(b.getAula().getNombre());
        r.setAsignacionCursoId(b.getAsignacionCurso().getId());
        r.setCursoId(b.getAsignacionCurso().getCurso().getId());
        r.setCursoNombre(b.getAsignacionCurso().getCurso().getNombre());
        r.setDocenteId(b.getAsignacionCurso().getDocente().getId());
        r.setDocenteNombre(b.getAsignacionCurso().getDocente().getNombre() + " " + b.getAsignacionCurso().getDocente().getApellidoPaterno());
        r.setSeccionId(b.getAsignacionCurso().getSeccion().getId());
        r.setSeccionNombre(b.getAsignacionCurso().getSeccion().getNombre());
        r.setGradoId(b.getAsignacionCurso().getSeccion().getGrado().getId());
        r.setGradoNombre(b.getAsignacionCurso().getSeccion().getGrado().getNombre());
        r.setAnio(b.getAsignacionCurso().getSeccion().getAnioAcademico().getAnio());
        return r;
    }
}