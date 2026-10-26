package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.dominio.LogError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LogErrorRepository extends JpaRepository<LogError, Long> {

    @Query("""
       SELECT l FROM LogError l
       WHERE (:fechaInicio IS NULL OR l.fechaHora >= :fechaInicio)
         AND (:fechaFin IS NULL OR l.fechaHora <= :fechaFin)
       """)
    Page<LogError> buscarConFiltros(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );
}