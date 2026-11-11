package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface BloqueHorarioRepository
        extends JpaRepository<BloqueHorario, Long>, BloqueHorarioRepositoryCustom {

    List<BloqueHorario> findByAsignacionCursoSeccionId(Long seccionId);
    List<BloqueHorario> findByAsignacionCursoSeccionGradoIdAndAsignacionCursoSeccionAnioAcademicoId(Long gradoId, Long anioId);
    List<BloqueHorario> findByAsignacionCursoDocenteIdAndAsignacionCursoSeccionAnioAcademicoId(Long docenteId, Long anioId);

    @Query("""
        SELECT b FROM BloqueHorario b
        WHERE b.diaSemana = :dia
          AND b.horaInicio < :horaFin
          AND b.horaFin > :horaInicio
          AND b.asignacionCurso.seccion.anioAcademico.id = :anioId
          AND (:excludeId IS NULL OR b.id <> :excludeId)
          AND b.asignacionCurso.docente.id = :docenteId
        """)
    List<BloqueHorario> findConflictoDocente(
            @Param("dia") DiaSemana dia,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("anioId") Long anioId,
            @Param("excludeId") Long excludeId,
            @Param("docenteId") Long docenteId);

    @Query("""
        SELECT b FROM BloqueHorario b
        WHERE b.diaSemana = :dia
          AND b.horaInicio < :horaFin
          AND b.horaFin > :horaInicio
          AND b.asignacionCurso.seccion.anioAcademico.id = :anioId
          AND (:excludeId IS NULL OR b.id <> :excludeId)
          AND b.asignacionCurso.seccion.id = :seccionId
        """)
    List<BloqueHorario> findConflictoSeccion(
            @Param("dia") DiaSemana dia,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("anioId") Long anioId,
            @Param("excludeId") Long excludeId,
            @Param("seccionId") Long seccionId);

    @Query("""
        SELECT b FROM BloqueHorario b
        WHERE b.diaSemana = :dia
          AND b.horaInicio < :horaFin
          AND b.horaFin > :horaInicio
          AND b.asignacionCurso.seccion.anioAcademico.id = :anioId
          AND (:excludeId IS NULL OR b.id <> :excludeId)
          AND b.aula.id = :aulaId
        """)
    List<BloqueHorario> findConflictoAula(
            @Param("dia") DiaSemana dia,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("anioId") Long anioId,
            @Param("excludeId") Long excludeId,
            @Param("aulaId") Long aulaId);

    /**
     * Bloques del mismo año académico que pertenecen a otras secciones.
     * Se usan para validar choques al reemplazar el horario completo de una sección.
     */
    @Query("""
        SELECT b FROM BloqueHorario b
        JOIN FETCH b.asignacionCurso a
        JOIN FETCH a.docente d
        JOIN FETCH a.seccion s
        JOIN FETCH b.aula au
        WHERE s.anioAcademico.id = :anioId
          AND s.id <> :seccionId
        """)
    List<BloqueHorario> findEnOtrasSecciones(
            @Param("anioId") Long anioId,
            @Param("seccionId") Long seccionId);

    /**
     * Todos los bloques de un año académico con asignación, curso, docente, sección,
     * grado y aula ya cargados. Permite detectar conflictos en memoria durante una
     * carga masiva en lugar de consultar la base por cada fila.
     */
    @Query("""
        SELECT b FROM BloqueHorario b
        JOIN FETCH b.asignacionCurso a
        JOIN FETCH a.curso c
        JOIN FETCH a.docente d
        JOIN FETCH a.seccion s
        JOIN FETCH s.grado g
        JOIN FETCH b.aula au
        WHERE s.anioAcademico.id = :anioId
        ORDER BY b.id
        """)
    List<BloqueHorario> findDelAnioConDetalle(@Param("anioId") Long anioId);

    @Query("""
        SELECT b FROM BloqueHorario b
        JOIN FETCH b.asignacionCurso a
        JOIN FETCH a.seccion s
        JOIN FETCH s.grado g
        JOIN FETCH s.anioAcademico an
        JOIN FETCH a.curso c
        JOIN FETCH a.docente d
        JOIN FETCH b.aula au
        WHERE (:anioId IS NULL OR an.id = :anioId)
          AND (:gradoId IS NULL OR g.id = :gradoId)
          AND (:seccionId IS NULL OR s.id = :seccionId)
          AND (:docenteId IS NULL OR d.id = :docenteId)
        ORDER BY g.nombre, s.nombre, b.diaSemana, b.horaInicio
        """)
    List<BloqueHorario> buscarParaExportar(
            @Param("anioId") Long anioId,
            @Param("gradoId") Long gradoId,
            @Param("seccionId") Long seccionId,
            @Param("docenteId") Long docenteId);
}