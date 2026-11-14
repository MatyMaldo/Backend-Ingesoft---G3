package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SesionClaseRepository extends JpaRepository<SesionClase, Long>, SesionClaseRepositoryCustom {
    List<SesionClase> findByBloqueHorarioId(Long bloqueHorarioId);
    List<SesionClase> findByBloqueHorarioAsignacionCursoSeccionIdAndFechaBetween(Long seccionId, LocalDate inicio, LocalDate fin);
    List<SesionClase> findByBloqueHorarioAsignacionCursoDocenteIdAndFechaBetween(Long docenteId, LocalDate inicio, LocalDate fin);
    boolean existsByBloqueHorarioIdAndFecha(Long bloqueHorarioId, LocalDate fecha);
    List<SesionClase> findByBloqueHorarioIdAndFecha(Long bloqueHorarioId, LocalDate fecha);

    @Query("""
            SELECT s FROM SesionClase s
            JOIN FETCH s.bloqueHorario b
            JOIN FETCH b.asignacionCurso ac
            JOIN FETCH ac.curso c
            JOIN FETCH ac.seccion sec
            JOIN FETCH sec.grado g
            JOIN FETCH ac.docente d
            WHERE (s.estado = :activa AND s.horaFin <= :ahora)
               OR (s.estado = :programada AND s.horaInicio <= :ahora)
            ORDER BY s.horaFin ASC
            """)
    List<SesionClase> buscarParaCierre(
            @Param("activa") EstadoSesion activa,
            @Param("programada") EstadoSesion programada,
            @Param("ahora") LocalDateTime ahora,
            Pageable pageable);

    /**
     * Sesiones que ya existen para esos bloques en el rango, en una sola consulta. Sustituye a consultar bloque por
     * bloque y fecha por fecha (una consulta por cada sesión de la vista previa o de la generación).
     */
    @Query("""
        SELECT new com.educoders.Gestion_Matriz_Horarios.persistencia.SesionExistente(
                   s.bloqueHorario.id, s.fecha, s.estado)
        FROM SesionClase s
        WHERE s.bloqueHorario.id IN :bloqueIds
          AND s.fecha BETWEEN :inicio AND :fin
        """)
    List<SesionExistente> findExistentes(@Param("bloqueIds") Collection<Long> bloqueIds,
                                         @Param("inicio") LocalDate inicio,
                                         @Param("fin") LocalDate fin);
}
