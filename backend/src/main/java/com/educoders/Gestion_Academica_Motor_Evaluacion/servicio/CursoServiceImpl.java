package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.CompetenciaRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.CursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ActualizarCursoRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearCursoRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CursoResponse;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CursoServiceImpl implements CursoService {

    private final CursoRepository cursoRepository;
    private final CompetenciaRepository competenciaRepository;
    private final AsignacionCursoRepository asignacionCursoRepository;
    private final ValidadorVentanaAcademica validadorVentana; // <-- NUEVO

    public CursoServiceImpl(CursoRepository cursoRepository,
                            CompetenciaRepository competenciaRepository,
                            AsignacionCursoRepository asignacionCursoRepository,
                            ValidadorVentanaAcademica validadorVentana) { // <-- Quitamos BimestreRepository
        this.cursoRepository = cursoRepository;
        this.competenciaRepository = competenciaRepository;
        this.asignacionCursoRepository = asignacionCursoRepository;
        this.validadorVentana = validadorVentana;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CursoResponse> listarTodos() {
        return cursoRepository.findAll().stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    @Override
    @Transactional
    public CursoResponse crear(CrearCursoRequest request) {
        // Uso del validador compartido
        validadorVentana.verificarVentanaAbierta("La ventana de gestión ha cerrado. El año académico ya inició.");

        if (cursoRepository.existsByNombre(request.nombre())) {
            throw new ConflictoException("Ya existe un curso con ese nombre en el catálogo global.");
        }

        Curso curso = new Curso();
        curso.setNombre(request.nombre());
        curso.setDescripcion(request.descripcion());

        return convertirARespuesta(cursoRepository.save(curso));
    }

    @Override
    @Transactional
    public CursoResponse actualizar(Long idCurso, ActualizarCursoRequest request) {
        validadorVentana.verificarVentanaAbierta("La ventana de gestión ha cerrado. El año académico ya inició.");

        Curso curso = cursoRepository.findById(idCurso)
                .orElseThrow(() -> RecursoNoEncontradoException.de("curso", idCurso));

        if (cursoRepository.existsByNombreAndIdNot(request.nombre(), idCurso)) {
            throw new ConflictoException("Ya existe otro curso con ese nombre en el catálogo global.");
        }

        curso.setNombre(request.nombre());
        curso.setDescripcion(request.descripcion());

        return convertirARespuesta(cursoRepository.save(curso));
    }

    @Override
    @Transactional
    public void eliminar(Long idCurso) {
        validadorVentana.verificarVentanaAbierta("La ventana de gestión ha cerrado. El año académico ya inició.");

        Curso curso = cursoRepository.findById(idCurso)
                .orElseThrow(() -> RecursoNoEncontradoException.de("curso", idCurso));

        boolean tieneCompetencias = competenciaRepository.existsByCursoId(idCurso);
        boolean tieneAsignaciones = asignacionCursoRepository.existsByCursoId(idCurso);

        if (tieneCompetencias || tieneAsignaciones) {
            throw new ReglaNegocioException("No se puede eliminar el curso porque ya tiene competencias o asignaciones de docentes activas.");
        }

        cursoRepository.delete(curso);
    }

    private CursoResponse convertirARespuesta(Curso curso) {
        return new CursoResponse(
                curso.getId(),
                curso.getNombre(),
                curso.getDescripcion()
        );
    }
}