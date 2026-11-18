package com.educoders.Gestion_Matricula_Pagos.persistencia;

import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.Mes;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompromisoPagoRepository extends JpaRepository<CompromisoPago, Long> {

    /**
     * Verifica si existe un compromiso de un determinado tipo (ej. MATRICULA) para la matrícula dada.
     */
    boolean existsByMatriculaIdAndTipoCompromiso(Long matriculaId, TipoCompromiso tipoCompromiso);
    Optional<CompromisoPago> findByMatriculaIdAndTipoCompromiso(Long matriculaId, TipoCompromiso tipoCompromiso);

    /** Compromisos de una matrícula. Se usa en el borrado y en el listado administrativo. */
    List<CompromisoPago> findByMatriculaId(Long matriculaId);

    /** Compromisos de varias matrículas en una sola consulta. */
    List<CompromisoPago> findByMatriculaIdIn(Collection<Long> matriculaIds);

    /**
     * HU-GEMAP-05: Verifica si ya existe una pensión emitida para una matrícula en un mes determinado.
     */
    boolean existsByMatriculaIdAndTipoCompromisoAndMes(Long matriculaId, TipoCompromiso tipoCompromiso, Mes mes);
    Optional<CompromisoPago> findByMatriculaIdAndTipoCompromisoAndMes(Long matriculaId, TipoCompromiso tipoCompromiso, Mes mes);

    /**
     * IDs de matrículas que ya tienen un compromiso del tipo y mes indicados.
     * Se usa para validar en lote que exista el mes anterior antes de emitir
     * las pensiones del mes objetivo. No filtra por estado.
     */
    @Query("""
            SELECT c.matricula.id FROM CompromisoPago c
            WHERE c.matricula.id IN :matriculaIds
              AND c.tipoCompromiso = :tipoCompromiso
              AND c.mes = :mes
            """)
    List<Long> findMatriculaIdsConCompromiso(
            @Param("matriculaIds") Collection<Long> matriculaIds,
            @Param("tipoCompromiso") TipoCompromiso tipoCompromiso,
            @Param("mes") Mes mes);

    /*
        Añadir métodos de consulta filtrados por estudiante y año académico:
    * */
    @Query("""
        SELECT c FROM CompromisoPago c 
        JOIN FETCH c.matricula m 
        JOIN FETCH m.seccion s 
        JOIN FETCH s.anioAcademico a 
        WHERE m.estudiante.id = :estudianteId 
          AND (:anio IS NULL OR a.anio = :anio)
        ORDER BY c.fechaEmision ASC, c.id ASC
    """)
    List<CompromisoPago> findByEstudianteAndAnioOpcional(
            @Param("estudianteId") Long estudianteId,
            @Param("anio") Integer anio
    );

    /**
     * Listado administrativo de compromisos con filtros opcionales.
     */
    @Query(value = """
            SELECT c FROM CompromisoPago c
            JOIN FETCH c.matricula m
            JOIN FETCH m.estudiante e
            JOIN FETCH m.seccion s
            JOIN FETCH s.grado g
            JOIN FETCH s.anioAcademico a
            WHERE (:anioAcademicoId IS NULL OR a.id = :anioAcademicoId)
              AND (:gradoId IS NULL OR g.id = :gradoId)
              AND (:seccionId IS NULL OR s.id = :seccionId)
              AND (:estudianteId IS NULL OR e.id = :estudianteId)
              AND (:estadoCompromiso IS NULL OR c.estado = :estadoCompromiso)
              AND (:tipoCompromiso IS NULL OR c.tipoCompromiso = :tipoCompromiso)
            """,
            countQuery = """
            SELECT COUNT(c) FROM CompromisoPago c
            WHERE (:anioAcademicoId IS NULL OR c.matricula.seccion.anioAcademico.id = :anioAcademicoId)
              AND (:gradoId IS NULL OR c.matricula.seccion.grado.id = :gradoId)
              AND (:seccionId IS NULL OR c.matricula.seccion.id = :seccionId)
              AND (:estudianteId IS NULL OR c.matricula.estudiante.id = :estudianteId)
              AND (:estadoCompromiso IS NULL OR c.estado = :estadoCompromiso)
              AND (:tipoCompromiso IS NULL OR c.tipoCompromiso = :tipoCompromiso)
            """)
    Page<CompromisoPago> listarAdmin(
            @Param("anioAcademicoId") Long anioAcademicoId,
            @Param("gradoId") Long gradoId,
            @Param("seccionId") Long seccionId,
            @Param("estudianteId") Long estudianteId,
            @Param("estadoCompromiso") EstadoCompromisoPago estadoCompromiso,
            @Param("tipoCompromiso") TipoCompromiso tipoCompromiso,
            Pageable pageable
    );
}