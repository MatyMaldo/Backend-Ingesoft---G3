package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matriz_Horarios.config.HorarioProperties;
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.exception.GuardadoBloquesConConflictosException;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        List<BloqueMasivoItemRequest> items = request.bloques();
        List<BloqueHorario> existentes = bloqueRepo.findByAsignacionCursoSeccionId(seccionId);
        Map<Long, BloqueHorario> existentesPorId = existentes.stream()
                .collect(Collectors.toMap(BloqueHorario::getId, b -> b));

        List<GuardadoSeccionErroresResponse.ConflictoBloque> errores = new ArrayList<>();

        // IDs repetidos dentro del payload.
        Map<Long, Integer> primerIndicePorId = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            BloqueMasivoItemRequest item = items.get(i);
            if (item == null) {
                errores.add(conflicto(null, i, "REFERENCIA_INVALIDA",
                        "El bloque en la posición " + (i + 1) + " está vacío.",
                        null, null));
                continue;
            }
            if (item.id() != null) {
                Integer indicePrevio = primerIndicePorId.putIfAbsent(item.id(), i);
                if (indicePrevio != null) {
                    errores.add(conflicto(item, i, "DUPLICADO",
                            "El bloque en la posición " + (i + 1)
                                    + " repite el id " + item.id()
                                    + " del bloque en la posición " + (indicePrevio + 1) + ".",
                            indicePrevio, null));
                }
            }
        }

        // Carga en lote de bloques, asignaciones y aulas referenciadas.
        Map<Long, BloqueHorario> bloquesPorId = new HashMap<>(existentesPorId);
        if (!primerIndicePorId.isEmpty()) {
            bloqueRepo.findAllById(primerIndicePorId.keySet())
                    .forEach(b -> bloquesPorId.put(b.getId(), b));
        }

        Set<Long> asignacionIds = items.stream()
                .filter(Objects::nonNull)
                .map(BloqueMasivoItemRequest::asignacionCursoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AsignacionCurso> asignacionesPorId = new HashMap<>();
        if (!asignacionIds.isEmpty()) {
            asignacionRepo.findAllById(asignacionIds)
                    .forEach(a -> asignacionesPorId.put(a.getId(), a));
        }

        Set<Long> aulaIds = items.stream()
                .filter(Objects::nonNull)
                .map(BloqueMasivoItemRequest::aulaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Aula> aulasPorId = new HashMap<>();
        if (!aulaIds.isEmpty()) {
            aulaRepo.findAllById(aulaIds).forEach(a -> aulasPorId.put(a.getId(), a));
        }

        List<BloqueValidado> validados = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            BloqueMasivoItemRequest item = items.get(i);
            if (item == null) {
                continue;
            }

            int erroresAntes = errores.size();

            if (item.id() != null && !existentesPorId.containsKey(item.id())) {
                if (!bloquesPorId.containsKey(item.id())) {
                    errores.add(conflicto(item, i, "REFERENCIA_INVALIDA",
                            "El bloque " + item.id() + " no existe.", null, null));
                } else {
                    errores.add(conflicto(item, i, "REFERENCIA_INVALIDA",
                            "El bloque " + item.id()
                                    + " no pertenece a la sección indicada.", null, null));
                }
            }

            AsignacionCurso asignacion = item.asignacionCursoId() == null
                    ? null
                    : asignacionesPorId.get(item.asignacionCursoId());
            if (item.asignacionCursoId() == null) {
                errores.add(conflicto(item, i, "REFERENCIA_INVALIDA",
                        "La asignación de curso es obligatoria.", null, null));
            } else if (asignacion == null) {
                errores.add(conflicto(item, i, "REFERENCIA_INVALIDA",
                        "La asignación " + item.asignacionCursoId() + " no existe.", null, null));
            } else if (asignacion.getSeccion() == null
                    || !seccionId.equals(asignacion.getSeccion().getId())) {
                errores.add(conflicto(item, i, "REFERENCIA_INVALIDA",
                        "La asignación " + item.asignacionCursoId()
                                + " no pertenece a la sección indicada.", null, null));
            }

            Aula aula = item.aulaId() == null ? null : aulasPorId.get(item.aulaId());
            if (item.aulaId() == null) {
                errores.add(conflicto(item, i, "REFERENCIA_INVALIDA",
                        "El aula es obligatoria.", null, null));
            } else if (aula == null) {
                errores.add(conflicto(item, i, "REFERENCIA_INVALIDA",
                        "El aula " + item.aulaId() + " no existe.", null, null));
            }

            if (item.diaSemana() == null) {
                errores.add(conflicto(item, i, "REFERENCIA_INVALIDA",
                        "El día de la semana es obligatorio.", null, null));
            }

            String mensajeHorario = mensajeHorarioInvalido(item.horaInicio(), item.horaFin());
            if (mensajeHorario != null) {
                errores.add(conflicto(item, i, "HORARIO_INVALIDO",
                        mensajeHorario, null, null));
            }

            if (errores.size() == erroresAntes) {
                validados.add(new BloqueValidado(
                        item.id(), i, asignacion, aula, item.diaSemana(),
                        item.horaInicio(), item.horaFin()));
            }
        }

        errores.addAll(conflictosEnPayload(validados));
        if (!validados.isEmpty()) {
            errores.addAll(conflictosEnOtrasSecciones(
                    validados,
                    bloqueRepo.findEnOtrasSecciones(request.anioAcademicoId(), seccionId)));
        }

        if (!errores.isEmpty()) {
            throw new GuardadoBloquesConConflictosException(
                    new GuardadoSeccionErroresResponse(errores));
        }

        Set<Long> idsPayload = validados.stream()
                .map(BloqueValidado::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

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
        String mensaje = mensajeHorarioInvalido(inicio, fin);
        if (mensaje != null) {
            throw new ReglaNegocioException(mensaje);
        }
    }

    /**
     * Devuelve el motivo por el que el horario no es válido, o {@code null} si
     * es correcto. Se usa tanto en el guardado individual (que lanza excepción)
     * como en el masivo (que acumula todos los errores).
     */
    private String mensajeHorarioInvalido(LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null) {
            return "La hora de inicio y fin son obligatorias.";
        }
        if (!fin.isAfter(inicio)) {
            return "La hora de fin debe ser posterior a la de inicio.";
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
            return "El bloque debe estar dentro de la jornada de "
                    + horarioProperties.getJornadaInicio() + " a "
                    + horarioProperties.getJornadaFin() + ".";
        }
        if (inicio.isBefore(recesoFin) && fin.isAfter(recesoInicio)) {
            return "El bloque no puede cruzar el receso de "
                    + horarioProperties.getRecesoInicio() + " a "
                    + horarioProperties.getRecesoFin() + ".";
        }
        return null;
    }

    private LocalTime parsearHoraConfig(String valor, String nombre) {
        try {
            return LocalTime.parse(valor, FORMATO_HORA);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Configuración inválida para app.horarios." + nombre + ": " + valor, e);
        }
    }

    private List<GuardadoSeccionErroresResponse.ConflictoBloque> conflictosEnPayload(
            List<BloqueValidado> bloques) {
        List<GuardadoSeccionErroresResponse.ConflictoBloque> errores = new ArrayList<>();

        for (int i = 0; i < bloques.size(); i++) {
            for (int j = i + 1; j < bloques.size(); j++) {
                BloqueValidado a = bloques.get(i);
                BloqueValidado b = bloques.get(j);
                if (a.diaSemana() != b.diaSemana()
                        || !seSuperponen(a.horaInicio(), a.horaFin(), b.horaInicio(), b.horaFin())) {
                    continue;
                }

                String dia = nombreDia(a.diaSemana());
                String rango = a.horaInicio().format(FORMATO_HORA)
                        + " a " + a.horaFin().format(FORMATO_HORA);

                boolean duplicado = a.asignacion().getId().equals(b.asignacion().getId())
                        && a.horaInicio().equals(b.horaInicio())
                        && a.horaFin().equals(b.horaFin());
                if (duplicado) {
                    errores.add(conflicto(b, "DUPLICADO",
                            "El bloque en la posición " + (j + 1)
                                    + " duplica al bloque en la posición " + (i + 1)
                                    + ": misma asignación, aula, día y horas.",
                            i, null));
                    continue;
                }

                if (a.asignacion().getDocente().getId()
                        .equals(b.asignacion().getDocente().getId())) {
                    errores.add(conflicto(b, "CONFLICTO_DOCENTE",
                            "El docente " + nombreDocente(a.asignacion())
                                    + " tiene dos bloques el " + dia + " de " + rango + ".",
                            i, null));
                }
                if (a.aula().getId().equals(b.aula().getId())) {
                    errores.add(conflicto(b, "CONFLICTO_AULA",
                            "El aula " + a.aula().getNombre()
                                    + " está ocupada en dos bloques el "
                                    + dia + " de " + rango + ".",
                            i, null));
                }
                errores.add(conflicto(b, "CONFLICTO_SECCION",
                        "La sección " + nombreSeccion(a.asignacion().getSeccion())
                                + " tiene dos bloques el " + dia + " de " + rango + ".",
                        i, null));
            }
        }

        return errores;
    }

    private List<GuardadoSeccionErroresResponse.ConflictoBloque> conflictosEnOtrasSecciones(
            List<BloqueValidado> validados,
            List<BloqueHorario> otrasSecciones) {
        List<GuardadoSeccionErroresResponse.ConflictoBloque> errores = new ArrayList<>();

        for (BloqueValidado validado : validados) {
            for (BloqueHorario existente : otrasSecciones) {
                if (existente.getDiaSemana() != validado.diaSemana()
                        || !seSuperponen(validado.horaInicio(), validado.horaFin(),
                        existente.getHoraInicio(), existente.getHoraFin())) {
                    continue;
                }

                String descripcion = descripcionBloqueExistente(existente);
                if (existente.getAsignacionCurso().getDocente().getId()
                        .equals(validado.asignacion().getDocente().getId())) {
                    errores.add(conflicto(validado, "CONFLICTO_DOCENTE",
                            "El docente " + nombreDocente(validado.asignacion())
                                    + " ya tiene clase en " + descripcion + ".",
                            null, existente.getId()));
                }
                if (existente.getAula().getId().equals(validado.aula().getId())) {
                    errores.add(conflicto(validado, "CONFLICTO_AULA",
                            "El aula " + existente.getAula().getNombre()
                                    + " está ocupada por " + descripcion + ".",
                            null, existente.getId()));
                }
            }
        }

        return errores;
    }

    private GuardadoSeccionErroresResponse.ConflictoBloque conflicto(
            BloqueMasivoItemRequest item,
            int indice,
            String tipo,
            String mensaje,
            Integer indiceConflicto,
            Long bloqueConflictoId) {
        return new GuardadoSeccionErroresResponse.ConflictoBloque(
                indice,
                item != null ? item.id() : null,
                indiceConflicto,
                bloqueConflictoId,
                item != null ? item.asignacionCursoId() : null,
                item != null ? item.aulaId() : null,
                item != null ? item.diaSemana() : null,
                item != null ? item.horaInicio() : null,
                item != null ? item.horaFin() : null,
                tipo,
                mensaje
        );
    }

    private GuardadoSeccionErroresResponse.ConflictoBloque conflicto(
            BloqueValidado validado,
            String tipo,
            String mensaje,
            Integer indiceConflicto,
            Long bloqueConflictoId) {
        return new GuardadoSeccionErroresResponse.ConflictoBloque(
                validado.indice(),
                validado.id(),
                indiceConflicto,
                bloqueConflictoId,
                validado.asignacion().getId(),
                validado.aula().getId(),
                validado.diaSemana(),
                validado.horaInicio(),
                validado.horaFin(),
                tipo,
                mensaje
        );
    }

    private String descripcionBloqueExistente(BloqueHorario bloque) {
        AsignacionCurso asignacion = bloque.getAsignacionCurso();
        return nombreSeccion(asignacion.getSeccion())
                + " (" + asignacion.getCurso().getNombre() + ") el "
                + nombreDia(bloque.getDiaSemana()) + " de "
                + bloque.getHoraInicio().format(FORMATO_HORA) + " a "
                + bloque.getHoraFin().format(FORMATO_HORA);
    }

    private String nombreDocente(AsignacionCurso asignacion) {
        var docente = asignacion.getDocente();
        String nombre = docente.getNombre() + " " + docente.getApellidoPaterno();
        if (docente.getApellidoMaterno() != null && !docente.getApellidoMaterno().isBlank()) {
            nombre += " " + docente.getApellidoMaterno();
        }
        return nombre;
    }

    private String nombreSeccion(Seccion seccion) {
        return seccion.getGrado().getNombre() + " " + seccion.getNombre();
    }

    private String nombreDia(DiaSemana dia) {
        return switch (dia) {
            case LUNES -> "lunes";
            case MARTES -> "martes";
            case MIERCOLES -> "miércoles";
            case JUEVES -> "jueves";
            case VIERNES -> "viernes";
        };
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
            int indice,
            AsignacionCurso asignacion,
            Aula aula,
            DiaSemana diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
    }
}