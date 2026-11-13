package com.educoders.Gestion_Asistencia.persistencia;

import com.educoders.Gestion_Asistencia.dominio.AsistenciaDocente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaDocenteRepository
        extends JpaRepository<AsistenciaDocente, Long> {

    Optional<AsistenciaDocente> findBySesionClaseId(Long sesionClaseId);

    @Query(value = """
            SELECT a FROM AsistenciaDocente a
            JOIN FETCH a.sesionClase s
            JOIN FETCH s.bloqueHorario b
            JOIN FETCH b.asignacionCurso ac
            JOIN FETCH ac.curso c
            JOIN FETCH ac.seccion sec
            JOIN FETCH sec.grado g
            WHERE a.docente.id = :docenteId
              AND (:desde IS NULL OR s.fecha >= :desde)
              AND (:hasta IS NULL OR s.fecha <= :hasta)
              AND (:cursoId IS NULL OR c.id = :cursoId)
            ORDER BY s.fecha DESC, s.horaInicio DESC
            """,
            countQuery = """
            SELECT COUNT(a) FROM AsistenciaDocente a
            JOIN a.sesionClase s
            JOIN s.bloqueHorario b
            JOIN b.asignacionCurso ac
            JOIN ac.curso c
            WHERE a.docente.id = :docenteId
              AND (:desde IS NULL OR s.fecha >= :desde)
              AND (:hasta IS NULL OR s.fecha <= :hasta)
              AND (:cursoId IS NULL OR c.id = :cursoId)
            """)
    Page<AsistenciaDocente> buscarHistorial(
            @Param("docenteId") Long docenteId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("cursoId") Long cursoId,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT c.id, c.nombre
            FROM AsistenciaDocente a
            JOIN a.sesionClase s
            JOIN s.bloqueHorario b
            JOIN b.asignacionCurso ac
            JOIN ac.curso c
            WHERE a.docente.id = :docenteId
              AND (:desde IS NULL OR s.fecha >= :desde)
              AND (:hasta IS NULL OR s.fecha <= :hasta)
            ORDER BY c.nombre
            """)
    List<Object[]> buscarCursosHistorial(
            @Param("docenteId") Long docenteId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT a.estado, COUNT(a)
            FROM AsistenciaDocente a
            JOIN a.sesionClase s
            JOIN s.bloqueHorario b
            JOIN b.asignacionCurso ac
            JOIN ac.curso c
            WHERE a.docente.id = :docenteId
              AND (:desde IS NULL OR s.fecha >= :desde)
              AND (:hasta IS NULL OR s.fecha <= :hasta)
              AND (:cursoId IS NULL OR c.id = :cursoId)
            GROUP BY a.estado
            """)
    List<Object[]> contarPorEstado(
            @Param("docenteId") Long docenteId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("cursoId") Long cursoId);
}
