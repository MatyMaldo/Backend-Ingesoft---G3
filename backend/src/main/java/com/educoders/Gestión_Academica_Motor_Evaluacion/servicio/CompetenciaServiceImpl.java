package com.educoders.Gestión_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestión_Academica_Motor_Evaluacion.dominio.Bimestre;
import com.educoders.Gestión_Academica_Motor_Evaluacion.dominio.Competencia;
import com.educoders.Gestión_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestión_Academica_Motor_Evaluacion.persistencia.BimestreRepository;
import com.educoders.Gestión_Academica_Motor_Evaluacion.persistencia.CompetenciaRepository;
import com.educoders.Gestión_Academica_Motor_Evaluacion.persistencia.CursoRepository;
import com.educoders.Gestión_Academica_Motor_Evaluacion.persistencia.NotaCompetenciaRepository;
import com.educoders.Gestión_Academica_Motor_Evaluacion.presentacion.dto.ActualizarCompetenciaRequest;
import com.educoders.Gestión_Academica_Motor_Evaluacion.presentacion.dto.CompetenciaResponse;
import com.educoders.Gestión_Academica_Motor_Evaluacion.presentacion.dto.CrearCompetenciaRequest;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CompetenciaServiceImpl implements CompetenciaService {

    private final CompetenciaRepository competenciaRepository;
    private final CursoRepository cursoRepository;
    private final NotaCompetenciaRepository notaCompetenciaRepository;
    private final BimestreRepository bimestreRepository;

    public CompetenciaServiceImpl(CompetenciaRepository competenciaRepository,
                                  CursoRepository cursoRepository,
                                  NotaCompetenciaRepository notaCompetenciaRepository,
                                  BimestreRepository bimestreRepository) {
        this.competenciaRepository = competenciaRepository;
        this.cursoRepository = cursoRepository;
        this.notaCompetenciaRepository = notaCompetenciaRepository;
        this.bimestreRepository = bimestreRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetenciaResponse> listarPorCurso(Long idCurso) {
        if (!cursoRepository.existsById(idCurso)) {
            throw RecursoNoEncontradoException.de("curso", idCurso);
        }
        return competenciaRepository.findByCursoId(idCurso).stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    @Override
    @Transactional
    public CompetenciaResponse crear(Long idCurso, CrearCompetenciaRequest request) {
        validarVentanaDeTiempo();

        Curso curso = cursoRepository.findById(idCurso)
                .orElseThrow(() -> RecursoNoEncontradoException.de("curso", idCurso));

        if (competenciaRepository.existsByCursoIdAndNombre(idCurso, request.nombre())) {
            throw new ConflictoException("Ya existe una competencia con ese nombre en este curso.");
        }

        Competencia competencia = new Competencia();
        competencia.setNombre(request.nombre());
        competencia.setDescripcion(request.descripcion());
        competencia.setCurso(curso);

        return convertirARespuesta(competenciaRepository.save(competencia));
    }

    @Override
    @Transactional
    public CompetenciaResponse actualizar(Long idCurso, Long idCompetencia, ActualizarCompetenciaRequest request) {
        validarVentanaDeTiempo();

        Competencia competencia = competenciaRepository.findByIdAndCursoId(idCompetencia, idCurso)
                .orElseThrow(() -> RecursoNoEncontradoException.de("competencia en el curso indicado", idCompetencia));

        if (competenciaRepository.existsByCursoIdAndNombreAndIdNot(idCurso, request.nombre(), idCompetencia)) {
            throw new ConflictoException("Ya existe otra competencia con ese nombre en este curso.");
        }

        competencia.setNombre(request.nombre());
        competencia.setDescripcion(request.descripcion());

        return convertirARespuesta(competenciaRepository.save(competencia));
    }

    @Override
    @Transactional
    public void eliminar(Long idCurso, Long idCompetencia) {
        validarVentanaDeTiempo();

        Competencia competencia = competenciaRepository.findByIdAndCursoId(idCompetencia, idCurso)
                .orElseThrow(() -> RecursoNoEncontradoException.de("competencia en el curso indicado", idCompetencia));

        // Escenario 5: Validar que no esté en uso
        if (notaCompetenciaRepository.existsByCompetenciaId(idCompetencia)) {
            throw new ReglaNegocioException("No se puede eliminar la competencia porque ya tiene notas registradas (está en uso).");
        }

        competenciaRepository.delete(competencia);
    }

    /**
     * Regla HU-EVAL-08 (Escenario 2): La gestión solo está permitida si el año académico
     * activo no ha iniciado (es decir, la fecha actual es menor a la fecha de inicio del BIM_1).
     */
    private void validarVentanaDeTiempo() {
        Bimestre primerBimestre = bimestreRepository.findPrimerBimestreDelAnioMasReciente()
                .orElseThrow(() -> new ReglaNegocioException("No hay un calendario académico configurado para gestionar competencias."));

        if (!LocalDate.now().isBefore(primerBimestre.getFechaInicio())) {
            throw new ReglaNegocioException("La gestión de competencias está cerrada. El año académico ya ha iniciado.");
        }
    }

    private CompetenciaResponse convertirARespuesta(Competencia competencia) {
        return new CompetenciaResponse(
                competencia.getId(),
                competencia.getNombre(),
                competencia.getDescripcion(),
                competencia.getCurso().getId()
        );
    }
}