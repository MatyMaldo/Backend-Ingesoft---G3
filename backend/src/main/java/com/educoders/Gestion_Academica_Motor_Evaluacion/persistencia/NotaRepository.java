package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotaRepository extends JpaRepository<Nota, Long> {

    // Para saber si ya existe una nota y proceder a actualizarla
    Optional<Nota> findByEstudianteIdAndEvaluacionId(Long idEstudiante, Long idEvaluacion);

    // NUEVO: Para traer todas las notas de un estudiante en un conjunto de evaluaciones
    List<Nota> findByEstudianteIdAndEvaluacionIdIn(Long idEstudiante, List<Long> idsEvaluaciones);
}