package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SesionClaseRepository extends JpaRepository<SesionClase, Long> {
    List<SesionClase> findByBloqueHorarioId(Long bloqueHorarioId);
    List<SesionClase> findByBloqueHorarioAsignacionCursoSeccionIdAndFechaBetween(Long seccionId, LocalDate inicio, LocalDate fin);
    boolean existsByBloqueHorarioIdAndFecha(Long bloqueHorarioId, LocalDate fecha);
    List<SesionClase> findByBloqueHorarioIdAndFecha(Long bloqueHorarioId, LocalDate fecha);
}