package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Infraestructura_Servicios.enums.Modulo;
import com.educoders.Seguridad_Auditoria.dominio.LogError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * Acceso de solo inserción y consulta al log de errores (HU-SAT-06, Esc. 5). Extiende {@link Repository} y no
 * {@code JpaRepository} a propósito: así no se exponen {@code delete*} ni el resto de operaciones de escritura.
 */
@org.springframework.stereotype.Repository
public interface LogErrorRepository extends Repository<LogError, Long> {

    LogError save(LogError logError);

    /**
     * {@code patron} es un patrón LIKE en minúsculas con {@code !} como escape (p. ej. {@code %pago%});
     * {@code %} coincide con todo. Busca en mensaje, endpoint (la operación) y nombre del usuario, como pide
     * HU-SAT-05. El tipo de error no es criterio de búsqueda: es una columna del listado.
     */
    @Query("""
       SELECT l FROM LogError l LEFT JOIN l.usuario u
       WHERE (:fechaInicio IS NULL OR l.fechaHora >= :fechaInicio)
         AND (:fechaFin IS NULL OR l.fechaHora <= :fechaFin)
         AND (:modulo IS NULL OR l.modulo = :modulo)
         AND (LOWER(l.mensaje) LIKE :patron ESCAPE '!'
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
