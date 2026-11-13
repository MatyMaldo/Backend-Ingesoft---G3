package com.educoders.Gestion_Asistencia.persistencia;

import com.educoders.Gestion_Asistencia.dominio.AsistenciaEstudiante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaEstudianteRepository
        extends JpaRepository<AsistenciaEstudiante, Long> {

    List<AsistenciaEstudiante> findBySesionClaseId(Long sesionClaseId);

    Optional<AsistenciaEstudiante> findBySesionClaseIdAndEstudianteId(
            Long sesionClaseId, Long estudianteId);

    @Query(value = """
            SELECT a FROM AsistenciaEstudiante a
            JOIN FETCH a.sesionClase s
            JOIN FETCH s.bloqueHorario b
            JOIN FETCH b.asignacionCurso ac
            JOIN FETCH ac.curso c
            JOIN FETCH ac.seccion sec
            JOIN FETCH sec.grado g
            JOIN FETCH ac.docente d
            WHERE a.estudiante.id = :estudianteId
              AND (:desde IS NULL OR s.fecha >= :desde)
              AND (:hasta IS NULL OR s.fecha <= :hasta)
              AND (:cursoId IS NULL OR c.id = :cursoId)
            ORDER BY s.fecha DESC, s.horaInicio DESC
            """,
            countQuery = """
            SELECT COUNT(a) FROM AsistenciaEstudiante a
            JOIN a.sesionClase s
            JOIN s.bloqueHorario b
            JOIN b.asignacionCurso ac
            JOIN ac.curso c
            WHERE a.estudiante.id = :estudianteId
              AND (:desde IS NULL OR s.fecha >= :desde)
              AND (:hasta IS NULL OR s.fecha <= :hasta)
              AND (:cursoId IS NULL OR c.id = :cursoId)
            """)
    Page<AsistenciaEstudiante> buscarHistorial(
            @Param("estudianteId") Long estudianteId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("cursoId") Long cursoId,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT c.id, c.nombre
            FROM AsistenciaEstudiante a
            JOIN a.sesionClase s
            JOIN s.bloqueHorario b
            JOIN b.asignacionCurso ac
            JOIN ac.curso c
            WHERE a.estudiante.id = :estudianteId
              AND (:desde IS NULL OR s.fecha >= :desde)
              AND (:hasta IS NULL OR s.fecha <= :hasta)
            ORDER BY c.nombre
            """)
    List<Object[]> buscarCursosHistorial(
            @Param("estudianteId") Long estudianteId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT a.estado, COUNT(a)
            FROM AsistenciaEstudiante a
            JOIN a.sesionClase s
            JOIN s.bloqueHorario b
            JOIN b.asignacionCurso ac
            JOIN ac.curso c
            WHERE a.estudiante.id = :estudianteId
              AND (:desde IS NULL OR s.fecha >= :desde)
              AND (:hasta IS NULL OR s.fecha <= :hasta)
              AND (:cursoId IS NULL OR c.id = :cursoId)
            GROUP BY a.estado
            """)
    List<Object[]> contarPorEstado(
            @Param("estudianteId") Long estudianteId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("cursoId") Long cursoId);
}
