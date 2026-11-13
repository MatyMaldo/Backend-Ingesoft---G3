package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SesionClaseRepository extends JpaRepository<SesionClase, Long> {
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
}