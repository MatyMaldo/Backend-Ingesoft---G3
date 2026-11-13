package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AsignacionCursoRepository extends JpaRepository<AsignacionCurso, Long> {
    boolean existsByCursoId(Long cursoId);

    Optional<AsignacionCurso> findByCursoIdAndSeccionId(Long cursoId, Long seccionId);
    boolean existsByCursoIdAndSeccionId(Long cursoId, Long seccionId);
    List<AsignacionCurso> findBySeccionId(Long seccionId);
    List<AsignacionCurso> findBySeccionGradoIdAndSeccionAnioAcademicoId(Long gradoId, Long anioId);
    List<AsignacionCurso> findByDocenteId(Long docenteId);

    /**
     * Todas las asignaciones de un año académico con curso, docente, sección y grado ya
     * cargados. Permite validar una carga masiva sin una consulta por fila.
     */
    @Query("""
        SELECT a FROM AsignacionCurso a
        JOIN FETCH a.curso
        JOIN FETCH a.docente
        JOIN FETCH a.seccion s
        JOIN FETCH s.grado
        WHERE s.anioAcademico.id = :anioId
        """)
    List<AsignacionCurso> findByAnioAcademicoIdConDetalle(@Param("anioId") Long anioId);

    /**
     * Asignaciones de un docente filtradas por año académico.
     * Evita duplicados en el contexto docente cuando el docente tiene asignaciones
     * en múltiples años académicos.
     */
    @Query("""
        SELECT a FROM AsignacionCurso a
        JOIN FETCH a.curso
        JOIN FETCH a.seccion s
        JOIN FETCH s.grado
        WHERE s.anioAcademico.id = :anioId
        AND a.docente.id = :docenteId
        """)
    List<AsignacionCurso> findByAnioAcademicoIdAndDocenteIdConDetalle(
            @Param("anioId") Long anioId,
            @Param("docenteId") Long docenteId
    );
}
