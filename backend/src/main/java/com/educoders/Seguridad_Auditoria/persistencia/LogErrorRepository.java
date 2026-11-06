package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Infraestructura_Servicios.enums.Modulo;
import com.educoders.Seguridad_Auditoria.dominio.LogError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LogErrorRepository extends JpaRepository<LogError, Long> {

    /**
     * {@code patron} es un patrón LIKE en minúsculas con {@code !} como escape (p. ej. {@code %pago%});
     * {@code %} coincide con todo. Busca en mensaje, tipo de error, endpoint y nombre del usuario.
     */
    @Query("""
       SELECT l FROM LogError l LEFT JOIN l.usuario u
       WHERE (:fechaInicio IS NULL OR l.fechaHora >= :fechaInicio)
         AND (:fechaFin IS NULL OR l.fechaHora <= :fechaFin)
         AND (:modulo IS NULL OR l.modulo = :modulo)
         AND (LOWER(l.mensaje) LIKE :patron ESCAPE '!'
              OR LOWER(l.tipoError) LIKE :patron ESCAPE '!'
              OR LOWER(COALESCE(l.endpoint, '')) LIKE :patron ESCAPE '!'
              OR LOWER(CONCAT(COALESCE(u.nombre, ''), ' ', COALESCE(u.apellidoPaterno, ''))) LIKE :patron ESCAPE '!')
       """)
    Page<LogError> buscarConFiltros(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("modulo") Modulo modulo,
            @Param("patron") String patron,
            Pageable pageable
    );
}