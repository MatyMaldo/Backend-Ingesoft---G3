package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Bimestre;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Competencia;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Evaluacion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.BimestreRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.CompetenciaRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.EvaluacionRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ContextoDocenteResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionResponse;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException; // Importante usar la excepción del estándar
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EvaluacionServiceImpl implements EvaluacionService {

    private final EvaluacionRepository evaluacionRepository;
    private final BimestreRepository bimestreRepository;
    private final CompetenciaRepository competenciaRepository;
    private final AsignacionCursoRepository asignacionCursoRepository;
    private final AnioAcademicoRepository anioAcademicoRepository;
    private final SeccionRepository seccionRepository;

    public EvaluacionServiceImpl(EvaluacionRepository evaluacionRepository,
                                 BimestreRepository bimestreRepository,
                                 CompetenciaRepository competenciaRepository,
                                 AsignacionCursoRepository asignacionCursoRepository,
                                 AnioAcademicoRepository anioAcademicoRepository,
                                 SeccionRepository seccionRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.bimestreRepository = bimestreRepository;
        this.competenciaRepository = competenciaRepository;
        this.asignacionCursoRepository = asignacionCursoRepository;
        this.anioAcademicoRepository = anioAcademicoRepository;
        this.seccionRepository = seccionRepository;
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

    @Override
    @Transactional(readOnly = true)
    public List<EvaluacionResponse> listarEvaluaciones(Optional<Long> idAsignacionCurso, Optional<Long> idSeccion, Optional<Long> idCurso) {
        List<Evaluacion> evaluaciones;

        if (idAsignacionCurso.isPresent()) {
            evaluaciones = evaluacionRepository.findByAsignacionCursoId(idAsignacionCurso.get());
        } else if (idSeccion.isPresent() && idCurso.isPresent()) {
            evaluaciones = evaluacionRepository.buscarEvaluacionesConDetalles(idSeccion.get(), idCurso.get());
        } else {
            throw new ReglaNegocioException("Debe proporcionar al menos idAsignacionCurso o ambos idSeccion e idCurso");
        }

        return evaluaciones.stream()
                .map(e -> new EvaluacionResponse(
                        e.getId(),
                        e.getNombre(),
                        e.getDescripcion(),
                        e.getFecha(),
                        e.getBimestre().getNombre().name(),
                        e.getCompetencia().getNombre(),
                        e.getAsignacionCurso().getCurso().getNombre(),
                        e.getAsignacionCurso().getSeccion().getNombre()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ContextoDocenteResponse obtenerContextoDocente(Docente docenteLogueado) {
        LocalDate hoy = LocalDate.now();

        // 1. Obtener año académico vigente
        Optional<AnioAcademico> anioVigenteOpt = anioAcademicoRepository
                .findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(hoy, hoy);

        if (anioVigenteOpt.isEmpty()) {
            throw new ReglaNegocioException("No hay un año académico vigente.");
        }

        AnioAcademico anioVigente = anioVigenteOpt.get();

        // 2. Obtener asignaciones del docente
        List<AsignacionCurso> asignaciones = asignacionCursoRepository.findByDocenteId(docenteLogueado.getId());

        // 3. Obtener bimestres del año vigente
        List<Bimestre> bimestres = bimestreRepository.findByAnioAcademicoId(anioVigente.getId());

        // 4. Obtener secciones del año vigente
        List<Seccion> secciones = seccionRepository.findByAnioAcademicoId(anioVigente.getId());

        // 5. Obtener competencias de todos los cursos del docente
        List<Long> idsCursos = asignaciones.stream()
                .map(a -> a.getCurso().getId())
                .distinct()
                .toList();

        List<Competencia> competencias = idsCursos.stream()
                .flatMap(idCurso -> competenciaRepository.findByCursoId(idCurso).stream())
                .distinct()
                .toList();

        // 6. Construir la respuesta
        ContextoDocenteResponse.AnioAcademicoInfo anioInfo = new ContextoDocenteResponse.AnioAcademicoInfo(
                anioVigente.getId(),
                anioVigente.getAnio(),
                anioVigente.getFechaInicio().toString(),
                anioVigente.getFechaFin().toString()
        );

        List<ContextoDocenteResponse.AsignacionInfo> asignacionesInfo = asignaciones.stream()
                .map(a -> new ContextoDocenteResponse.AsignacionInfo(
                        a.getId(),
                        a.getCurso().getId(),
                        a.getCurso().getNombre(),
                        a.getSeccion().getId(),
                        a.getSeccion().getNombre(),
                        a.getSeccion().getGrado().getId(),
                        a.getSeccion().getGrado().getNombre()
                ))
                .toList();

        List<ContextoDocenteResponse.BimestreInfo> bimestresInfo = bimestres.stream()
                .map(b -> new ContextoDocenteResponse.BimestreInfo(
                        b.getId(),
                        b.getNombre(),
                        b.getFechaInicio().toString(),
                        b.getFechaFin().toString(),
                        hoy.isAfter(b.getFechaFin())
                ))
                .toList();

        List<ContextoDocenteResponse.SeccionInfo> seccionesInfo = secciones.stream()
                .map(s -> new ContextoDocenteResponse.SeccionInfo(
                        s.getId(),
                        s.getNombre(),
                        s.getGrado().getId(),
                        s.getGrado().getNombre()
                ))
                .toList();

        List<ContextoDocenteResponse.CompetenciaInfo> competenciasInfo = competencias.stream()
                .map(c -> new ContextoDocenteResponse.CompetenciaInfo(
                        c.getId(),
                        c.getNombre(),
                        c.getCurso().getId()
                ))
                .toList();

        return new ContextoDocenteResponse(
                anioInfo,
                asignacionesInfo,
                bimestresInfo,
                seccionesInfo,
                competenciasInfo
        );
    }
}