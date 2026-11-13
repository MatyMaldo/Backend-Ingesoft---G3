
package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Bimestre;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Competencia;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.NotaCompetencia;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.BimestreRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.CompetenciaRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.LibretaNotasRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.NotaCompetenciaRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CalificacionCompetenciaRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.NotaCompetenciaResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.NotasCompetenciasResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.RegistrarNotasCompetenciasRequest;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.*;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleAuditoriaDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementa HU-EVAL-07.
 * Registra las notas finales de las competencias sin
 * calcular promedios ni notas globales del curso.
 */
@Service
public class NotasCompetenciasServiceImpl
        implements NotasCompetenciasService {

    private final AsignacionCursoRepository asignacionCursoRepository;
    private final BimestreRepository bimestreRepository;
    private final CompetenciaRepository competenciaRepository;
    private final NotaCompetenciaRepository notaCompetenciaRepository;
    private final LibretaNotasRepository libretaNotasRepository;
    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final AuditoriaService auditoriaService;

    public NotasCompetenciasServiceImpl(
            AsignacionCursoRepository asignacionCursoRepository,
            BimestreRepository bimestreRepository,
            CompetenciaRepository competenciaRepository,
            NotaCompetenciaRepository notaCompetenciaRepository,
            LibretaNotasRepository libretaNotasRepository,
            EstudianteRepository estudianteRepository,
            MatriculaRepository matriculaRepository,
            AuditoriaService auditoriaService) {

        this.asignacionCursoRepository = asignacionCursoRepository;
        this.bimestreRepository = bimestreRepository;
        this.competenciaRepository = competenciaRepository;
        this.notaCompetenciaRepository = notaCompetenciaRepository;
        this.libretaNotasRepository = libretaNotasRepository;
        this.estudianteRepository = estudianteRepository;
        this.matriculaRepository = matriculaRepository;
        this.auditoriaService = auditoriaService;
    }

    /**
     * Registra o modifica las notas finales de las competencias.
     * Solo permite operar al docente asignado, después del
     * bimestre y antes del cierre o generación de la libreta.
     */
    @Override
    @Transactional
    public NotasCompetenciasResponse registrarNotas(
            RegistrarNotasCompetenciasRequest request,
            Docente docente) {

        // 1. Validar la asignación del curso.
        AsignacionCurso asignacion = asignacionCursoRepository
                .findById(request.idAsignacionCurso())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La asignación de curso no existe."));

        if (!asignacion.getDocente().getId().equals(docente.getId())) {
            throw new ReglaNegocioException(
                    "No tiene permiso para registrar notas de este curso.");
        }

        // 2. Validar el bimestre y año académico.
        Bimestre bimestre = bimestreRepository
                .findById(request.idBimestre())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El bimestre no existe."));

        Long idAnioSeccion = asignacion.getSeccion()
                .getAnioAcademico().getId();

        if (!bimestre.getAnioAcademico().getId()
                .equals(idAnioSeccion)) {
            throw new ReglaNegocioException(
                    "El bimestre no corresponde al año de la sección.");
        }

        LocalDate hoy = LocalDate.now();

        if (!hoy.isAfter(bimestre.getFechaFin())) {
            throw new ReglaNegocioException(
                    "Las notas de competencias se registran una vez finalizado el bimestre.");
        }

        if (hoy.isAfter(bimestre.getAnioAcademico().getFechaFin())) {
            throw new ReglaNegocioException(
                    "No se pueden registrar notas después del cierre del año académico.");
        }

        // 3. Validar estudiante activo y matrícula confirmada.
        Estudiante estudiante = estudianteRepository
                .findById(request.idEstudiante())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El estudiante no existe."));

        if (!estudiante.isActivo()) {
            throw new ReglaNegocioException(
                    "No se pueden registrar notas de estudiantes inactivos.");
        }

        Matricula matricula = matriculaRepository
                .findByEstudianteIdAndSeccionId(
                        estudiante.getId(),
                        asignacion.getSeccion().getId()
                )
                .orElseThrow(() -> new ReglaNegocioException(
                        "El estudiante no pertenece a esta sección."));

        if (matricula.getEstado() != EstadoMatricula.CONFIRMADA) {
            throw new ReglaNegocioException(
                    "El estudiante no tiene matrícula confirmada.");
        }

        // 4. Bloquear el registro si la libreta está generada.
        boolean libretaGenerada = libretaNotasRepository
                .existsByEstudianteIdAndBimestreIdAndEstado(
                        estudiante.getId(),
                        bimestre.getId(),
                        EstadoLibreta.GENERADA
                );

        if (libretaGenerada) {
            throw new ReglaNegocioException(
                    "Las notas están bloqueadas porque la libreta ya fue generada.");
        }

        // 5. Validar todas las competencias del curso.
        List<Competencia> competenciasCurso = competenciaRepository
                .findByCursoId(asignacion.getCurso().getId());

        if (competenciasCurso.isEmpty()) {
            throw new ReglaNegocioException(
                    "El curso no tiene competencias registradas.");
        }

        if (request.calificaciones() == null
                || request.calificaciones().isEmpty()) {
            throw new ReglaNegocioException(
                    "Debe registrar las competencias del curso.");
        }

        Set<Long> idsEsperados = competenciasCurso.stream()
                .map(Competencia::getId)
                .collect(Collectors.toSet());

        Set<Long> idsRecibidos = request.calificaciones().stream()
                .map(CalificacionCompetenciaRequest::idCompetencia)
                .collect(Collectors.toSet());

        if (idsRecibidos.size() != request.calificaciones().size()) {
            throw new ReglaNegocioException(
                    "No se puede registrar una competencia repetida.");
        }

        if (!idsEsperados.equals(idsRecibidos)) {
            throw new ReglaNegocioException(
                    "Debe registrar todas las competencias del curso, sin agregar otras.");
        }

        // 6. Registrar o modificar cada competencia.
        List<NotasCompetenciasResponse.NotaCompetenciaRegistrada>
                resultados = new ArrayList<>();

        for (CalificacionCompetenciaRequest calificacion
                : request.calificaciones()) {

            Competencia competencia = competenciasCurso.stream()
                    .filter(c -> c.getId().equals(
                            calificacion.idCompetencia()))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException(
                            "Competencia no encontrada en el curso."));

            String conclusion = calificacion.conclusionDescriptiva();

            if (conclusion == null || conclusion.trim().isEmpty()) {
                throw new ReglaNegocioException(
                        "La conclusión descriptiva es obligatoria. Competencia: "
                                + competencia.getNombre());
            }

            conclusion = conclusion.trim();

            if (conclusion.length() > 120) {
                throw new ReglaNegocioException(
                        "La conclusión descriptiva no puede superar los 120 caracteres. Competencia: "
                                + competencia.getNombre());
            }

            if (calificacion.valorNota() == null) {
                throw new ReglaNegocioException(
                        "La nota es obligatoria. Valores: AD, A, B, C.");
            }

            NotaCompetencia nota = notaCompetenciaRepository
                    .findByEstudianteIdAndBimestreIdAndCompetenciaId(
                            estudiante.getId(),
                            bimestre.getId(),
                            competencia.getId()
                    )
                    .orElseGet(NotaCompetencia::new);

            boolean esNueva = nota.getId() == null;

            String valorAnterior = nota.getValor() == null
                    ? null : nota.getValor().name();

            String conclusionAnterior = nota.getConclusionDescriptiva();

            // Evitar operaciones innecesarias si nada cambió.
            if (!esNueva
                    && nota.getValor() == calificacion.valorNota()
                    && conclusion.equals(conclusionAnterior)) {

                resultados.add(
                        new NotasCompetenciasResponse.NotaCompetenciaRegistrada(
                                nota.getId(),
                                competencia.getId(),
                                nota.getValor(),
                                nota.getConclusionDescriptiva()
                        )
                );
                continue;
            }

            nota.setEstudiante(estudiante);
            nota.setBimestre(bimestre);
            nota.setCompetencia(competencia);
            nota.setValor(calificacion.valorNota());
            nota.setConclusionDescriptiva(conclusion);
            nota.setFechaRegistro(hoy);

            // Generar descripción automática basada en la nota
            nota.setDescripcion(generarDescripcionAutomatica(calificacion.valorNota()));

            NotaCompetencia guardada =
                    notaCompetenciaRepository.saveAndFlush(nota);

            // 7. Registrar auditoría de creación o actualización.
            TipoAccionAuditoria accion = esNueva
                    ? TipoAccionAuditoria.CREAR
                    : TipoAccionAuditoria.ACTUALIZAR;

            RegistrarAuditoriaRequestDTO auditoria =
                    new RegistrarAuditoriaRequestDTO(
                            docente.getId(),
                            CategoriaAuditoria.NOTAS,
                            accion,
                            "NotaCompetencia",
                            guardada.getId(),
                            "Registro o modificación de competencia "
                                    + competencia.getNombre(),
                            List.of(
                                    new DetalleAuditoriaDTO(
                                            "valor",
                                            valorAnterior,
                                            guardada.getValor().name()
                                    ),
                                    new DetalleAuditoriaDTO(
                                            "conclusionDescriptiva",
                                            conclusionAnterior,
                                            guardada.getConclusionDescriptiva()
                                    )
                            )
                    );

            auditoriaService.registrarAuditoria(auditoria);

            resultados.add(
                    new NotasCompetenciasResponse.NotaCompetenciaRegistrada(
                            guardada.getId(),
                            competencia.getId(),
                            guardada.getValor(),
                            guardada.getConclusionDescriptiva()
                    )
            );
        }

        // 8. Respuesta sin promedio ni nota final de curso.
        return new NotasCompetenciasResponse(
                asignacion.getId(),
                estudiante.getId(),
                bimestre.getId(),
                resultados
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotaCompetenciaResponse> listarNotasCompetencias(
            Optional<Long> idEstudiante,
            Optional<Long> idBimestre,
            Optional<Long> idCurso) {

        List<NotaCompetencia> notas;

        if (idEstudiante.isPresent() && idBimestre.isPresent() && idCurso.isPresent()) {
            notas = notaCompetenciaRepository.findByEstudianteIdAndBimestreIdAndCompetenciaCursoId(
                    idEstudiante.get(),
                    idBimestre.get(),
                    idCurso.get()
            );
        } else if (idEstudiante.isPresent() && idCurso.isPresent()) {
            notas = notaCompetenciaRepository.findByEstudianteIdAndCompetenciaCursoIdAndBimestreAnioAcademicoId(
                    idEstudiante.get(),
                    idCurso.get(),
                    null
            );
        } else {
            throw new ReglaNegocioException(
                    "Debe proporcionar al menos idEstudiante, idBimestre e idCurso juntos, o idEstudiante e idCurso"
            );
        }

        return notas.stream()
                .map(nota -> new NotaCompetenciaResponse(
                        nota.getCompetencia().getId(),
                        nota.getCompetencia().getNombre(),
                        nota.getBimestre().getNombre().name(),
                        nota.getValor(),
                        nota.getConclusionDescriptiva()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Genera una descripción automática basada en la nota.
     * Basado en los patrones de los inserts de la BD.
     */
    private String generarDescripcionAutomatica(EscalaNota nota) {
        return switch (nota) {
            case AD -> "Logro destacado";
            case A -> "Logro esperado";
            case B -> "En proceso";
            case C -> "En inicio";
        };
    }
}
