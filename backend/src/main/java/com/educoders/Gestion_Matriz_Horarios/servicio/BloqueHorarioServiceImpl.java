package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matriz_Horarios.config.HorarioProperties;
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.persistencia.AulaRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.*;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BloqueHorarioServiceImpl implements BloqueHorarioService {

    private final BloqueHorarioRepository bloqueRepo;
    private final AsignacionCursoRepository asignacionRepo;
    private final AulaRepository aulaRepo;
    private final SeccionRepository seccionRepo;
    private final PeriodoMatriculaRepository periodoRepo;
    private final AuditoriaService auditoriaService;
    private final HorarioProperties horarioProperties;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    public BloqueHorarioServiceImpl(BloqueHorarioRepository bloqueRepo,
                                    AsignacionCursoRepository asignacionRepo,
                                    AulaRepository aulaRepo,
                                    SeccionRepository seccionRepo,
                                    PeriodoMatriculaRepository periodoRepo,
                                    AuditoriaService auditoriaService,
                                    HorarioProperties horarioProperties) {
        this.bloqueRepo = bloqueRepo;
        this.asignacionRepo = asignacionRepo;
        this.aulaRepo = aulaRepo;
        this.seccionRepo = seccionRepo;
        this.periodoRepo = periodoRepo;
        this.auditoriaService = auditoriaService;
        this.horarioProperties = horarioProperties;
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

        validarHorarioPermitido(request.getHoraInicio(), request.getHoraFin());

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

        validarHorarioPermitido(request.getHoraInicio(), request.getHoraFin());

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

    @Override
    @Transactional
    public GuardadoMasivoBloquesResponse guardarMasivo(Long seccionId,
                                                      GuardarBloquesSeccionRequest request) {
        if (request == null || request.anioAcademicoId() == null || request.bloques() == null) {
            throw new ReglaNegocioException("El año académico y la lista de bloques son obligatorios.");
        }

        LocalDate hoy = LocalDate.now();

        PeriodoMatricula periodo = periodoRepo.findByAnioAcademicoId(request.anioAcademicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró un periodo de matrícula para el año académico indicado."));
        if (!VentanaMatricula.crudBloquesHorarioAbierta(periodo, hoy)) {
            throw new ReglaNegocioException(
                    "La programación de horarios está cerrada: solo se habilita durante el periodo de matrícula.");
        }

        Seccion seccion = seccionRepo.findById(seccionId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("sección", seccionId));
        if (seccion.getAnioAcademico() == null
                || !request.anioAcademicoId().equals(seccion.getAnioAcademico().getId())) {
            throw new ReglaNegocioException("La sección no pertenece al año académico indicado.");
        }

        List<BloqueHorario> existentes = bloqueRepo.findByAsignacionCursoSeccionId(seccionId);
        Map<Long, BloqueHorario> existentesPorId = existentes.stream()
                .collect(Collectors.toMap(BloqueHorario::getId, b -> b));

        Set<Long> idsPayload = new HashSet<>();
        for (BloqueMasivoItemRequest item : request.bloques()) {
            if (item != null && item.id() != null && !idsPayload.add(item.id())) {
                throw new ReglaNegocioException(
                        "El bloque con id " + item.id() + " está repetido en la solicitud.");
            }
        }
        for (Long id : idsPayload) {
            if (!existentesPorId.containsKey(id)) {
                bloqueRepo.findById(id)
                        .orElseThrow(() -> RecursoNoEncontradoException.de("bloque horario", id));
                throw new ReglaNegocioException(
                        "El bloque " + id + " no pertenece a la sección indicada.");
            }
        }

        Map<Long, AsignacionCurso> asignaciones = asignacionRepo.findBySeccionId(seccionId).stream()
                .collect(Collectors.toMap(AsignacionCurso::getId, a -> a));

        Set<Long> aulaIds = request.bloques().stream()
                .filter(item -> item != null)
                .map(BloqueMasivoItemRequest::aulaId)
                .collect(Collectors.toSet());
        Map<Long, Aula> aulas = aulaRepo.findAllById(aulaIds).stream()
                .collect(Collectors.toMap(Aula::getId, a -> a));
        Set<Long> aulasFaltantes = new HashSet<>(aulaIds);
        aulasFaltantes.removeAll(aulas.keySet());
        if (!aulasFaltantes.isEmpty()) {
            throw RecursoNoEncontradoException.de("aula", aulasFaltantes.iterator().next());
        }

        List<BloqueValidado> validados = new ArrayList<>();
        for (BloqueMasivoItemRequest item : request.bloques()) {
            if (item == null || item.diaSemana() == null) {
                throw new ReglaNegocioException(
                        "Cada bloque debe indicar día, asignación, aula y horas.");
            }
            AsignacionCurso asignacion = asignaciones.get(item.asignacionCursoId());
            if (asignacion == null) {
                asignacionRepo.findById(item.asignacionCursoId())
                        .orElseThrow(() -> RecursoNoEncontradoException.de(
                                "asignación", item.asignacionCursoId()));
                throw new ReglaNegocioException(
                        "La asignación " + item.asignacionCursoId()
                                + " no pertenece a la sección indicada.");
            }

            Aula aula = aulas.get(item.aulaId());
            if (aula == null) {
                throw RecursoNoEncontradoException.de("aula", item.aulaId());
            }

            validarHorarioPermitido(item.horaInicio(), item.horaFin());
            validados.add(new BloqueValidado(
                    item.id(), asignacion, aula, item.diaSemana(),
                    item.horaInicio(), item.horaFin()));
        }

        validarConflictosPayload(validados);
        validarConflictosOtrasSecciones(
                validados,
                bloqueRepo.findEnOtrasSecciones(request.anioAcademicoId(), seccionId));

        List<BloqueHorario> aEliminar = existentes.stream()
                .filter(b -> !idsPayload.contains(b.getId()))
                .toList();
        bloqueRepo.deleteAll(aEliminar);

        int creados = 0;
        int actualizados = 0;
        List<GuardadoMasivoBloquesResponse.BloqueMasivoItemResponse> respuesta = new ArrayList<>();

        for (BloqueValidado validado : validados) {
            BloqueHorario bloque;
            if (validado.id() == null) {
                bloque = new BloqueHorario();
                creados++;
            } else {
                bloque = existentesPorId.get(validado.id());
                actualizados++;
            }

            bloque.setAsignacionCurso(validado.asignacion());
            bloque.setAula(validado.aula());
            bloque.setDiaSemana(validado.diaSemana());
            bloque.setHoraInicio(validado.horaInicio());
            bloque.setHoraFin(validado.horaFin());

            BloqueHorario guardado = bloqueRepo.save(bloque);
            respuesta.add(new GuardadoMasivoBloquesResponse.BloqueMasivoItemResponse(
                    guardado.getId(),
                    validado.asignacion().getId(),
                    validado.aula().getId(),
                    validado.diaSemana(),
                    validado.horaInicio(),
                    validado.horaFin()
            ));
        }

        auditoriaService.registrarAuditoria(
                new com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO(
                        null,
                        com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria.HORARIOS,
                        com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria.ACTUALIZAR,
                        "BloqueHorario",
                        seccionId,
                        "Guardado masivo de bloques de la sección " + seccion.getNombre()
                                + ": " + creados + " creados, " + actualizados
                                + " actualizados, " + aEliminar.size() + " eliminados",
                        List.of()
                ));

        return new GuardadoMasivoBloquesResponse(
                respuesta, creados, actualizados, aEliminar.size());
    }

    private void validarHorarioPermitido(LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null) {
            throw new ReglaNegocioException("La hora de inicio y fin son obligatorias.");
        }
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la de inicio.");
        }

        LocalTime jornadaInicio = parsearHoraConfig(
                horarioProperties.getJornadaInicio(), "jornada-inicio");
        LocalTime jornadaFin = parsearHoraConfig(
                horarioProperties.getJornadaFin(), "jornada-fin");
        LocalTime recesoInicio = parsearHoraConfig(
                horarioProperties.getRecesoInicio(), "receso-inicio");
        LocalTime recesoFin = parsearHoraConfig(
                horarioProperties.getRecesoFin(), "receso-fin");

        if (inicio.isBefore(jornadaInicio) || fin.isAfter(jornadaFin)) {
            throw new ReglaNegocioException(
                    "El bloque debe estar dentro de la jornada de "
                            + horarioProperties.getJornadaInicio() + " a "
                            + horarioProperties.getJornadaFin() + ".");
        }
        if (inicio.isBefore(recesoFin) && fin.isAfter(recesoInicio)) {
            throw new ReglaNegocioException(
                    "El bloque no puede cruzar el receso de "
                            + horarioProperties.getRecesoInicio() + " a "
                            + horarioProperties.getRecesoFin() + ".");
        }
    }

    private LocalTime parsearHoraConfig(String valor, String nombre) {
        try {
            return LocalTime.parse(valor, FORMATO_HORA);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Configuración inválida para app.horarios." + nombre + ": " + valor, e);
        }
    }

    private void validarConflictosPayload(List<BloqueValidado> bloques) {
        for (int i = 0; i < bloques.size(); i++) {
            for (int j = i + 1; j < bloques.size(); j++) {
                BloqueValidado a = bloques.get(i);
                BloqueValidado b = bloques.get(j);
                if (a.diaSemana() != b.diaSemana()
                        || !seSuperponen(a.horaInicio(), a.horaFin(), b.horaInicio(), b.horaFin())) {
                    continue;
                }
                if (a.asignacion().getDocente().getId().equals(b.asignacion().getDocente().getId())) {
                    throw new ConflictoException(
                            "El docente tiene dos bloques en el mismo horario.");
                }
                if (a.aula().getId().equals(b.aula().getId())) {
                    throw new ConflictoException(
                            "El aula está ocupada en el mismo horario.");
                }
                throw new ConflictoException(
                        "La sección tiene dos bloques en el mismo horario.");
            }
        }
    }

    private void validarConflictosOtrasSecciones(List<BloqueValidado> validados,
                                                 List<BloqueHorario> otrasSecciones) {
        for (BloqueValidado validado : validados) {
            for (BloqueHorario existente : otrasSecciones) {
                if (existente.getDiaSemana() != validado.diaSemana()
                        || !seSuperponen(validado.horaInicio(), validado.horaFin(),
                        existente.getHoraInicio(), existente.getHoraFin())) {
                    continue;
                }
                if (existente.getAsignacionCurso().getDocente().getId()
                        .equals(validado.asignacion().getDocente().getId())) {
                    throw new ConflictoException(
                            "El docente ya tiene un bloque en ese horario en otra sección.");
                }
                if (existente.getAula().getId().equals(validado.aula().getId())) {
                    throw new ConflictoException(
                            "El aula ya está asignada en ese horario en otra sección.");
                }
            }
        }
    }

    private boolean seSuperponen(LocalTime inicioA, LocalTime finA,
                                 LocalTime inicioB, LocalTime finB) {
        return inicioA.isBefore(finB) && inicioB.isBefore(finA);
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

    private record BloqueValidado(
            Long id,
            AsignacionCurso asignacion,
            Aula aula,
            DiaSemana diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
    }
}