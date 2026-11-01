package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Evaluacion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Nota;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.NotaCompetencia;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.EvaluacionRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.NotaCompetenciaRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.NotaRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionNotaResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.NotaCompetenciaResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.RevisionNotasResponse;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevisionNotasServiceImpl implements RevisionNotasService {

    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final NotaRepository notaRepository;
    private final NotaCompetenciaRepository notaCompetenciaRepository;

    public RevisionNotasServiceImpl(EstudianteRepository estudianteRepository,
                                    MatriculaRepository matriculaRepository,
                                    EvaluacionRepository evaluacionRepository,
                                    NotaRepository notaRepository,
                                    NotaCompetenciaRepository notaCompetenciaRepository) {
        this.estudianteRepository = estudianteRepository;
        this.matriculaRepository = matriculaRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.notaRepository = notaRepository;
        this.notaCompetenciaRepository = notaCompetenciaRepository;
    }

    /**
     * Obtiene el consolidado de notas de evaluaciones y competencias de un estudiante.
     * Regla de negocio: HU-EVAL-03. Solo muestra información del año de consulta
     * (el año más reciente en el que el alumno tiene matrícula confirmada).
     */
    @Override
    @Transactional(readOnly = true)
    public RevisionNotasResponse obtenerNotasEstudiante(Long idEstudiante, Long idCurso, Usuario usuarioAutenticado) {

        // Validar que el usuario autenticado sea el estudiante o un apoderado a su cargo
        if (usuarioAutenticado instanceof Estudiante) {
            if (!usuarioAutenticado.getId().equals(idEstudiante)) {
                throw new ReglaNegocioException("No tienes permiso para ver las notas de otro estudiante.");
            }
        } else if (usuarioAutenticado instanceof Apoderado) {
            if (!estudianteRepository.existsByIdAndApoderadoId(idEstudiante, usuarioAutenticado.getId())) {
                throw new ReglaNegocioException("No tienes permiso para ver las notas de este estudiante.");
            }
        } else {
            throw new ReglaNegocioException("Solo estudiantes y apoderados pueden consultar notas.");
        }

        // 1. Validar que el estudiante existe
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new ReglaNegocioException("El estudiante no existe."));

        // 2. CAMBIO: Obtener la matrícula más reciente usando JOIN FETCH
        List<Matricula> matriculas = matriculaRepository.buscarMatriculasConSeccionYAnio(idEstudiante);
        Matricula matriculaConsulta = matriculas.stream()
                .filter(m -> m.getEstado() == EstadoMatricula.CONFIRMADA)
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("El estudiante no cuenta con una matrícula confirmada."));

        Long idAnioAcademico = matriculaConsulta.getSeccion().getAnioAcademico().getId();
        Long idSeccion = matriculaConsulta.getSeccion().getId();

        // 3. CAMBIO: Buscar las evaluaciones usando JOIN FETCH
        List<Evaluacion> evaluaciones = evaluacionRepository.buscarEvaluacionesConDetalles(idSeccion, idCurso);

        if (evaluaciones.isEmpty()) {
            throw new ReglaNegocioException("No hay evaluaciones registradas para este curso.");
        }

        // 4. Obtener las notas del alumno para esas evaluaciones
        List<Long> idsEvaluaciones = evaluaciones.stream().map(Evaluacion::getId).collect(Collectors.toList());
        List<Nota> notas = notaRepository.findByEstudianteIdAndEvaluacionIdIn(idEstudiante, idsEvaluaciones);

        // Mapear las notas a un diccionario para búsqueda rápida (idEvaluacion -> Nota)
        Map<Long, Nota> mapaNotas = notas.stream()
                .collect(Collectors.toMap(n -> n.getEvaluacion().getId(), n -> n));

        // 5. Construir la lista de evaluaciones con sus notas
        List<EvaluacionNotaResponse> evaluacionesResponse = evaluaciones.stream().map(eval -> {
            Nota nota = mapaNotas.get(eval.getId());
            return new EvaluacionNotaResponse(
                    eval.getId(),
                    eval.getNombre(),
                    eval.getDescripcion(),
                    eval.getFecha(),
                    eval.getBimestre().getNombre().name(),
                    eval.getCompetencia().getNombre(),
                    nota != null ? nota.getValor() : null // null significa "no hay nota registrada"
            );
        }).collect(Collectors.toList());

        // 6. CAMBIO: Obtener las notas de competencia usando JOIN FETCH
        List<NotaCompetencia> notasCompetencias = notaCompetenciaRepository
                .buscarNotasCompetenciaConDetalles(idEstudiante, idCurso, idAnioAcademico);

        List<NotaCompetenciaResponse> notasCompResponse = notasCompetencias.stream()
                .map(nc -> new NotaCompetenciaResponse(
                        nc.getCompetencia().getNombre(),
                        nc.getBimestre().getNombre().name(),
                        nc.getValor(),
                        nc.getConclusionDescriptiva()
                )).collect(Collectors.toList());

        // 7. Retornar consolidado
        return new RevisionNotasResponse(
                estudiante.getId(),
                estudiante.getNombre() + " " + estudiante.getApellidoPaterno(),
                matriculaConsulta.getSeccion().getGrado().getNombre(),
                matriculaConsulta.getSeccion().getNombre(),
                evaluacionesResponse,
                notasCompResponse
        );
    }
}
