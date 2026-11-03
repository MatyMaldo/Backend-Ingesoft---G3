package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsignacionCursoRepository extends JpaRepository<AsignacionCurso, Long> {
    boolean existsByCursoId(Long cursoId);

    Optional<AsignacionCurso> findByCursoIdAndSeccionId(Long cursoId, Long seccionId);
    boolean existsByCursoIdAndSeccionId(Long cursoId, Long seccionId);
    List<AsignacionCurso> findBySeccionId(Long seccionId);
    List<AsignacionCurso> findBySeccionGradoIdAndSeccionAnioAcademicoId(Long gradoId, Long anioId);
    List<AsignacionCurso> findByDocenteId(Long docenteId);
}
