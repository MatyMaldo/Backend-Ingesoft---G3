package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.NotaCompetencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface NotaCompetenciaRepository extends JpaRepository<NotaCompetencia, Long> {
    // Trae las notas finales de competencia de un alumno para un curso en un año específico
    List<NotaCompetencia> findByEstudianteIdAndCompetenciaCursoIdAndBimestreAnioAcademicoId(
            Long idEstudiante, Long idCurso, Long idAnioAcademico);

    // Regla de negocio: validar si está en uso antes de eliminar
    boolean existsByCompetenciaId(Long competenciaId);

    // Usamos JOIN FETCH para traer la competencia y el bimestre de golpe
    @Query("SELECT nc FROM NotaCompetencia nc " +
            "JOIN FETCH nc.competencia c " +
            "JOIN FETCH nc.bimestre b " +
            "WHERE nc.estudiante.id = :idEstudiante " +
            "AND c.curso.id = :idCurso " +
            "AND b.anioAcademico.id = :idAnioAcademico")
    List<NotaCompetencia> buscarNotasCompetenciaConDetalles(
            @Param("idEstudiante") Long idEstudiante,
            @Param("idCurso") Long idCurso,
            @Param("idAnioAcademico") Long idAnioAcademico);


    // HU-EVAL-04 y HU-EVAL-05:
    // Obtener las notas por competencia de un estudiante
    // correspondientes al bimestre de su libreta.
    @Query("""
            SELECT nc
            FROM NotaCompetencia nc
            JOIN FETCH nc.competencia c
            JOIN FETCH c.curso curso
            JOIN FETCH nc.bimestre b
            WHERE nc.estudiante.id = :idEstudiante
              AND b.id = :idBimestre
            ORDER BY curso.nombre, c.nombre
            """)
    List<NotaCompetencia> buscarNotasParaLibreta(
            @Param("idEstudiante") Long idEstudiante,
            @Param("idBimestre") Long idBimestre
    );


    /**
     * Busca una nota final de competencia para
     * actualizarla cuando ya exista.
     */
    Optional<NotaCompetencia>
    findByEstudianteIdAndBimestreIdAndCompetenciaId(
            Long idEstudiante,
            Long idBimestre,
            Long idCompetencia
    );

    /**
     * Consulta las notas de un estudiante en un
     * curso y bimestre específicos.
     */
    List<NotaCompetencia>
    findByEstudianteIdAndBimestreIdAndCompetenciaCursoId(
            Long idEstudiante,
            Long idBimestre,
            Long idCurso
    );


}
