package com.educoders.Gestión_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestión_Academica_Motor_Evaluacion.dominio.Competencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompetenciaRepository extends JpaRepository<Competencia, Long> {

    List<Competencia> findByCursoId(Long cursoId);

    // Valida si ya existe una competencia con ese nombre en el mismo curso
    boolean existsByCursoIdAndNombre(Long cursoId, String nombre);

    // Para actualizar, excluyendo la competencia actual
    boolean existsByCursoIdAndNombreAndIdNot(Long cursoId, String nombre, Long idCompetencia);

    Optional<Competencia> findByIdAndCursoId(Long id, Long cursoId);
}
