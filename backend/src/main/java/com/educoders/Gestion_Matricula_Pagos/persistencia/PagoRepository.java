package com.educoders.Gestion_Matricula_Pagos.persistencia;

import com.educoders.Gestion_Matricula_Pagos.dominio.Pago;
import com.educoders.Infraestructura_Servicios.enums.EstadoPago;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.compromisoPago c
        JOIN FETCH p.apoderadoPagador a
        WHERE c.id = :compromisoId
        ORDER BY p.fechaRegistro DESC, p.id DESC
    """)
    List<Pago> findByCompromisoPagoId(@Param("compromisoId") Long compromisoId);

    // Verifica si ya existe un pago pendiente de validación para el compromiso
    boolean existsByCompromisoPagoIdAndEstado(Long compromisoId, EstadoPago estado);

    /*
        HU-GEMAP-08: Listado de pagos para admin
    */
    @Query(value = """
            SELECT p FROM Pago p
            JOIN p.compromisoPago c
            JOIN c.matricula m
            JOIN m.seccion s
            WHERE s.anioAcademico.id = :anioAcademicoId
              AND s.grado.id = :gradoId
              AND s.id = :seccionId
              AND (:estudianteId IS NULL OR m.estudiante.id = :estudianteId)
              AND (:estado IS NULL OR p.estado = :estado)
              AND (:tipoCompromiso IS NULL OR c.tipoCompromiso = :tipoCompromiso)
            """,
            countQuery = """
            SELECT COUNT(p) FROM Pago p
            JOIN p.compromisoPago c
            JOIN c.matricula m
            JOIN m.seccion s
            WHERE s.anioAcademico.id = :anioAcademicoId
              AND s.grado.id = :gradoId
              AND s.id = :seccionId
              AND (:estudianteId IS NULL OR m.estudiante.id = :estudianteId)
              AND (:estado IS NULL OR p.estado = :estado)
              AND (:tipoCompromiso IS NULL OR c.tipoCompromiso = :tipoCompromiso)
            """)
    Page<Pago> listarPagosFiltrados(
            @Param("anioAcademicoId") Long anioAcademicoId,
            @Param("gradoId") Long gradoId,
            @Param("seccionId") Long seccionId,
            @Param("estudianteId") Long estudianteId,
            @Param("estado") EstadoPago estado,
            @Param("tipoCompromiso") TipoCompromiso tipoCompromiso,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.compromisoPago c
        JOIN FETCH c.matricula m
        JOIN FETCH m.seccion s
        JOIN FETCH s.grado g
        JOIN FETCH m.estudiante e
        JOIN FETCH p.apoderadoPagador a
        LEFT JOIN FETCH p.usuarioValidador u
        WHERE p.id = :id
    """)
    Optional<Pago> findByIdConDetalle(@Param("id") Long id);
}