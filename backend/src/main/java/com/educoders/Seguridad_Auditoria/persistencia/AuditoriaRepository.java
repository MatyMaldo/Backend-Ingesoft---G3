package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.dominio.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Acceso de solo inserción y consulta a los registros de auditoría (HU-SAT-04, Esc. 1).
 * Extiende {@link Repository} y no {@code JpaRepository} a propósito: así no se exponen
 * {@code delete*}, {@code deleteAll*} ni el resto de operaciones de escritura heredadas.
 */
@org.springframework.stereotype.Repository
public interface AuditoriaRepository extends Repository<Auditoria, Long> {

    Auditoria saveAndFlush(Auditoria auditoria);

    Optional<Auditoria> findById(Long id);

    /**
     * {@code patron} es un patrón LIKE en minúsculas con {@code !} como escape (p. ej. {@code %pago%});
     * {@code %} coincide con todo. Busca en la descripción y en el nombre del usuario que ejecutó la acción
     * (los procesos automáticos, sin usuario, se encuentran buscando "sistema").
     */
    @Query("""
            SELECT a FROM Auditoria a WHERE
            (:categoria IS NULL OR a.categoria = :categoria) AND
            (:accion IS NULL OR a.accion = :accion) AND
            (:fechaInicio IS NULL OR a.fechaHora >= :fechaInicio) AND
            (:fechaFin IS NULL OR a.fechaHora <= :fechaFin) AND
            (LOWER(COALESCE(a.descripcion, '')) LIKE :patron ESCAPE '!'
             OR (a.usuarioId IS NULL AND 'sistema' LIKE :patron ESCAPE '!')
             OR a.usuarioId IN (
                SELECT u.id FROM Usuario u
                WHERE LOWER(CONCAT(COALESCE(u.nombre, ''), ' ', COALESCE(u.apellidoPaterno, ''), ' ',
                                   COALESCE(u.apellidoMaterno, ''))) LIKE :patron ESCAPE '!'))
            """)
    Page<Auditoria> buscarConFiltros(
            @Param("categoria") CategoriaAuditoria categoria,
            @Param("accion") TipoAccionAuditoria accion,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("patron") String patron,
            Pageable pageable);

    Page<Auditoria> findByEntidadAndEntidadIdOrderByFechaHoraAsc(String entidad, Long entidadId, Pageable pageable);
}
