package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnioAcademicoRepository extends JpaRepository<AnioAcademico, Long> {

    /**
     * Busca el año académico vigente (fechaInicio <= hoy <= fechaFin)
     */
    Optional<AnioAcademico> findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(LocalDate fechaInicio, LocalDate fechaFin);

    /**
     * Busca años académicos futuros ordenados por fecha de inicio ascendente
     */
    List<AnioAcademico> findByFechaInicioGreaterThanOrderByFechaInicioAsc(LocalDate fechaInicio);
}