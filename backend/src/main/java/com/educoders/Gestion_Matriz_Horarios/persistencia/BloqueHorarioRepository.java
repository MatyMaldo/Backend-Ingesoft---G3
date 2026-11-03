package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface BloqueHorarioRepository extends JpaRepository<BloqueHorario, Long> {

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
}