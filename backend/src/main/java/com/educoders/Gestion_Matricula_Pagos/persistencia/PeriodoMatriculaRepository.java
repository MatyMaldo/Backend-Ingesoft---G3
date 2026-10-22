package com.educoders.Gestion_Matricula_Pagos.persistencia;

import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PeriodoMatriculaRepository extends JpaRepository<PeriodoMatricula, Long> {

    Optional<PeriodoMatricula> findByAnioAcademicoId(Long anioAcademicoId);

    /**
     * Obtiene el período de matrícula activo para una fecha determinada.
     */
    @Query("SELECT p FROM PeriodoMatricula p WHERE :fecha BETWEEN p.fechaInicio AND p.fechaFin")
    Optional<PeriodoMatricula> findPeriodoVigenteEnFecha(@Param("fecha") LocalDate fecha);
}