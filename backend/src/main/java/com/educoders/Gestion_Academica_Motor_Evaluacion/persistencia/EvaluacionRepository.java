package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    List<Evaluacion> findByAsignacionCursoSeccionIdAndAsignacionCursoCursoId(
            Long seccionId, Long cursoId);

    @Query("SELECT e FROM Evaluacion e " +
            "JOIN FETCH e.bimestre b " +
            "JOIN FETCH e.competencia c " +
            "WHERE e.asignacionCurso.seccion.id = :idSeccion " +
            "AND e.asignacionCurso.curso.id = :idCurso")
    List<Evaluacion> buscarEvaluacionesConDetalles(@Param("idSeccion") Long idSeccion, @Param("idCurso") Long idCurso);
}
