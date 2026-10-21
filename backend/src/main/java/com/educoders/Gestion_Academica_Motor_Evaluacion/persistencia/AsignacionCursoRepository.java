package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionCursoRepository extends JpaRepository<AsignacionCurso, Long> {
    boolean existsByCursoId(Long cursoId);
}
