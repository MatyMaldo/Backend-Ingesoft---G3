
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
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HU-EVAL-06: Valida las condiciones académicas y de autorización
 * necesarias para generar una libreta de notas.
 */
@Component
public class ValidadorGeneracionLibreta {

    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final BimestreRepository bimestreRepository;
    private final LibretaNotasRepository libretaNotasRepository;
    private final AsignacionCursoRepository asignacionCursoRepository;
    private final CompetenciaRepository competenciaRepository;
    private final NotaCompetenciaRepository notaCompetenciaRepository;

    public ValidadorGeneracionLibreta(
            EstudianteRepository estudianteRepository,
            MatriculaRepository matriculaRepository,
            BimestreRepository bimestreRepository,
            LibretaNotasRepository libretaNotasRepository,
            AsignacionCursoRepository asignacionCursoRepository,
            CompetenciaRepository competenciaRepository,
            NotaCompetenciaRepository notaCompetenciaRepository) {

        this.estudianteRepository = estudianteRepository;
        this.matriculaRepository = matriculaRepository;
        this.bimestreRepository = bimestreRepository;
        this.libretaNotasRepository = libretaNotasRepository;
        this.asignacionCursoRepository = asignacionCursoRepository;
        this.competenciaRepository = competenciaRepository;
        this.notaCompetenciaRepository = notaCompetenciaRepository;
    }

    /**
     * Valida que el tutor pueda generar la libreta y que todas
     * las calificaciones por competencia estén completas.
     */
    @Transactional(readOnly = true)
    public ContextoGeneracion validar(
            Long idEstudiante,
            Long idBimestre,
            Docente tutor) {

        // 1. Verificar usuario e identificadores.
        if (tutor == null || !tutor.isActivo()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un docente activo puede generar libretas.");
        }

        if (idEstudiante == null || idEstudiante <= 0
                || idBimestre == null || idBimestre <= 0) {
            throw new ReglaNegocioException(
                    "Debe especificar un estudiante y un bimestre válidos.");
        }

        // 2. Comprobar que el estudiante exista y esté activo.
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El estudiante no existe."));

        if (!estudiante.isActivo()) {
            throw new ReglaNegocioException(
                    "No se pueden generar libretas de estudiantes inactivos.");
        }

        // 3. Verificar que el bimestre haya finalizado.
        Bimestre bimestre = bimestreRepository.findById(idBimestre)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El bimestre no existe."));

        if (!LocalDate.now().isAfter(bimestre.getFechaFin())) {
            throw new ReglaNegocioException(
                    "La libreta solo puede generarse al finalizar un bimestre.");
        }

        // 4. Buscar la matrícula confirmada del año académico.
        List<Matricula> matriculas = matriculaRepository
                .buscarPorEstudianteYAnioId(
                        idEstudiante,
                        bimestre.getAnioAcademico().getId(),
                        EstadoMatricula.CONFIRMADA
                );

        if (matriculas.isEmpty()) {
            throw new ReglaNegocioException(
                    "El estudiante no tiene matrícula confirmada en este año académico.");
        }

        // 5. Comprobar que pertenece a una sección del tutor.
        Matricula matricula = matriculas.stream()
                .filter(m -> m.getSeccion().getTutor() != null
                        && tutor.getId().equals(
                        m.getSeccion().getTutor().getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "El estudiante no pertenece a una sección a su cargo."));

        // 6. Impedir libretas duplicadas.
        if (libretaNotasRepository.existsByEstudianteIdAndBimestreId(
                idEstudiante, idBimestre)) {
            throw new ReglaNegocioException(
                    "Ya existe una libreta para este estudiante y bimestre.");
        }

        // 7. Obtener los cursos asignados a la sección.
        List<AsignacionCurso> asignaciones = asignacionCursoRepository
                .findBySeccionId(matricula.getSeccion().getId());

        if (asignaciones.isEmpty()) {
            throw new ReglaNegocioException(
                    "La sección no tiene cursos asignados.");
        }

        // 8. Obtener las notas registradas en HU-EVAL-07.
        List<NotaCompetencia> notas = notaCompetenciaRepository
                .buscarNotasParaLibreta(idEstudiante, idBimestre);

        Map<Long, NotaCompetencia> notasPorCompetencia = new HashMap<>();

        for (NotaCompetencia nota : notas) {
            notasPorCompetencia.put(
                    nota.getCompetencia().getId(),
                    nota
            );
        }

        // 9. Verificar todas las competencias de cada curso.
        Set<Long> cursosRevisados = new HashSet<>();

        for (AsignacionCurso asignacion : asignaciones) {

            Long idCurso = asignacion.getCurso().getId();

            if (!cursosRevisados.add(idCurso)) {
                continue;
            }

            List<Competencia> competencias = competenciaRepository
                    .findByCursoId(idCurso);

            if (competencias.isEmpty()) {
                throw new ReglaNegocioException(
                        "El curso " + asignacion.getCurso().getNombre()
                                + " no tiene competencias registradas.");
            }

            for (Competencia competencia : competencias) {

                NotaCompetencia nota = notasPorCompetencia.get(
                        competencia.getId());

                if (nota == null || nota.getValor() == null
                        || nota.getConclusionDescriptiva() == null
                        || nota.getConclusionDescriptiva().isBlank()) {

                    throw new ReglaNegocioException(
                            "Todavía no se han registrado todas las notas "
                                    + "de competencias del curso del estudiante "
                                    + "en este bimestre. Curso: "
                                    + asignacion.getCurso().getNombre()
                                    + ". Competencia: "
                                    + competencia.getNombre());
                }
            }
        }

        // 10. Entregar los datos validados al servicio.
        return new ContextoGeneracion(
                estudiante,
                bimestre,
                matricula,
                List.copyOf(notas)
        );
    }

    /**
     * Datos académicos validados para construir una libreta.
     */
    public record ContextoGeneracion(
            Estudiante estudiante,
            Bimestre bimestre,
            Matricula matricula,
            List<NotaCompetencia> notas
    ) {
    }
}
