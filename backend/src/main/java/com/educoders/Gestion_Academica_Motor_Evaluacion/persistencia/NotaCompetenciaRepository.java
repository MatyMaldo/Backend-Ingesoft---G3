package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.NotaCompetencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaCompetenciaRepository extends JpaRepository<NotaCompetencia, Long> {

    // Regla de negocio: validar si está en uso antes de eliminar
    boolean existsByCompetenciaId(Long competenciaId);
}
