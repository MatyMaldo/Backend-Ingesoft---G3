package com.educoders.Gestión_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestión_Academica_Motor_Evaluacion.dominio.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CursoRepository extends JpaRepository<Curso, Long> {
}
