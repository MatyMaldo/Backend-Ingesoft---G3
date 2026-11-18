package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeccionRepository extends JpaRepository<Seccion, Long> {
    boolean existsByGradoIdAndAnioAcademicoIdAndNombre(Long gradoId, Long anioAcademicoId, String nombre);
    boolean existsByAnioAcademicoIdAndTutorId(Long anioAcademicoId, Long tutorId);
    // Los listados muestran grado, año y tutor de cada sección: el grafo los trae en la misma consulta.
    @EntityGraph(attributePaths = {"grado", "anioAcademico", "tutor"})
    List<Seccion> findByGradoIdAndAnioAcademicoId(Long gradoId, Long anioAcademicoId);

    @Override
    @EntityGraph(attributePaths = {"grado", "anioAcademico", "tutor"})
    List<Seccion> findAll();

    Optional<Seccion> findByGradoIdAndAnioAcademicoIdAndNombre(Long gradoId, Long anioAcademicoId, String nombre);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Seccion s " +
           "WHERE s.grado.id = :gradoId " +
           "AND s.anioAcademico.id = :anioAcademicoId " +
           "AND LOWER(s.nombre) = LOWER(:nombre)")
    boolean existsByGradoIdAndAnioAcademicoIdAndNombreIgnoreCase(@Param("gradoId") Long gradoId,
                                                                 @Param("anioAcademicoId") Long anioAcademicoId,
                                                                 @Param("nombre") String nombre);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Seccion s " +
           "WHERE s.grado.id = :gradoId " +
           "AND s.anioAcademico.id = :anioAcademicoId " +
           "AND LOWER(s.nombre) = LOWER(:nombre) " +
           "AND s.id != :seccionId")
    boolean existsByGradoIdAndAnioAcademicoIdAndNombreIgnoreCaseAndIdNot(@Param("gradoId") Long gradoId,
                                                                         @Param("anioAcademicoId") Long anioAcademicoId,
                                                                         @Param("nombre") String nombre,
                                                                         @Param("seccionId") Long seccionId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Seccion s " +
           "WHERE s.anioAcademico.id = :anioAcademicoId " +
           "AND s.tutor.id = :tutorId " +
           "AND s.id != :seccionId")
    boolean existsByAnioAcademicoIdAndTutorIdAndIdNot(@Param("anioAcademicoId") Long anioAcademicoId,
                                                      @Param("tutorId") Long tutorId,
                                                      @Param("seccionId") Long seccionId);

    @EntityGraph(attributePaths = {"grado", "anioAcademico", "tutor"})
    List<Seccion> findByAnioAcademicoId(Long anioAcademicoId);
    List<Seccion> findByTutorId(Long tutorId);

}