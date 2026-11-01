package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Bimestre;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Competencia;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Evaluacion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.BimestreRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.CompetenciaRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.EvaluacionRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionResponse;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException; // Importante usar la excepción del estándar
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EvaluacionServiceImpl implements EvaluacionService {

    private final EvaluacionRepository evaluacionRepository;
    private final BimestreRepository bimestreRepository;
    private final CompetenciaRepository competenciaRepository;
    private final AsignacionCursoRepository asignacionCursoRepository;

    public EvaluacionServiceImpl(EvaluacionRepository evaluacionRepository,
                                 BimestreRepository bimestreRepository,
                                 CompetenciaRepository competenciaRepository,
                                 AsignacionCursoRepository asignacionCursoRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.bimestreRepository = bimestreRepository;
        this.competenciaRepository = competenciaRepository;
        this.asignacionCursoRepository = asignacionCursoRepository;
    }

    /**
     * Registra una nueva evaluación en el sistema.
     * Regla de negocio: HU-EVAL-01. Valida que el docente dicte el curso, que el año
     * esté vigente, que la competencia pertenezca al curso y calcula el bimestre.
     */
    @Override
    @Transactional
    public EvaluacionResponse crearEvaluacion(EvaluacionRequest request, Docente docenteLogueado) {

        // 1. Validar que la asignación existe y PERTENECE AL DOCENTE LOGUEADO
        AsignacionCurso asignacion = asignacionCursoRepository.findById(request.idAsignacionCurso())
                .orElseThrow(() -> new ReglaNegocioException("Asignación de curso no encontrada"));

        if (!asignacion.getDocente().getId().equals(docenteLogueado.getId())) {
            throw new ReglaNegocioException("No tiene permiso para crear evaluaciones en este curso.");
        }

        // 2. Validar que el Año Académico esté vigente
        if (!asignacion.getSeccion().getAnioAcademico().estaVigente()) {
            throw new ReglaNegocioException("La creación de evaluaciones solo está disponible con el año académico vigente.");
        }

        // 3. Validar que la Competencia existe y PERTENECE AL CURSO DE LA ASIGNACIÓN
        Competencia competencia = competenciaRepository.findById(request.idCompetencia())
                .orElseThrow(() -> new ReglaNegocioException("Competencia no encontrada"));

        if (!competencia.getCurso().getId().equals(asignacion.getCurso().getId())) {
            throw new ReglaNegocioException("La competencia seleccionada no pertenece al curso de esta asignación.");
        }

        // 4. Buscar el bimestre vigente SOLO EN EL AÑO ACADÉMICO DE LA SECCIÓN
        Long idAnioAcademico = asignacion.getSeccion().getAnioAcademico().getId();
        Optional<Bimestre> bimestreOpt = bimestreRepository
                .findByAnioAcademicoIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                        idAnioAcademico, request.fecha(), request.fecha());

        if (bimestreOpt.isEmpty()) {
            throw new ReglaNegocioException("La fecha debe estar dentro de un bimestre del año académico.");
        }

        // 5. Crear la evaluación
        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setNombre(request.nombre());
        evaluacion.setDescripcion(request.descripcion());
        evaluacion.setFecha(request.fecha());
        evaluacion.setBimestre(bimestreOpt.get());
        evaluacion.setCompetencia(competencia);
        evaluacion.setAsignacionCurso(asignacion);

        Evaluacion evaluacionGuardada = evaluacionRepository.save(evaluacion);

        // 6. Construir la respuesta
        return new EvaluacionResponse(
                evaluacionGuardada.getId(),
                evaluacionGuardada.getNombre(),
                evaluacionGuardada.getDescripcion(),
                evaluacionGuardada.getFecha(),
                evaluacionGuardada.getBimestre().getNombre().name(),
                evaluacionGuardada.getCompetencia().getNombre(),
                evaluacionGuardada.getAsignacionCurso().getCurso().getNombre(),
                evaluacionGuardada.getAsignacionCurso().getSeccion().getNombre()
        );
    }
}