package com.educoders.Gestion_Matricula_Pagos.persistencia;

import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
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
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    /**
     * Cuenta cuántas matrículas en los estados especificados ocupan capacidad en la sección.
     */
    long countBySeccionIdAndEstadoIn(Long seccionId, Collection<EstadoMatricula> estados);

    /**
     * Verifica si el estudiante ya cuenta con matrícula previa en los estados indicados
     * dentro del año académico al que pertenece la sección.
     */
    boolean existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoIn(
            Long estudianteId,
            Long anioAcademicoId,
            Collection<EstadoMatricula> estados
    );

    List<Matricula> findBySeccionId(Long seccionId);

    Optional<Matricula> findByEstudianteIdAndSeccionId(Long estudianteId, Long seccionId);

    List<Matricula> findByEstudianteIdOrderBySeccionAnioAcademicoAnioDesc(Long estudianteId);

    @Query("SELECT m FROM Matricula m " +
            "JOIN FETCH m.seccion s " +
            "JOIN FETCH s.anioAcademico a " +
            "JOIN FETCH s.grado g " +
            "WHERE m.estudiante.id = :idEstudiante " +
            "ORDER BY a.anio DESC")
    List<Matricula> buscarMatriculasConSeccionYAnio(@Param("idEstudiante") Long idEstudiante);

    /**
     * Busca las matrículas de un estudiante para un año académico (por su id) y estado.
     * La más reciente queda primero, por si existiera más de una.
     */
    @Query("SELECT m FROM Matricula m " +
            "JOIN FETCH m.seccion s " +
            "JOIN FETCH s.anioAcademico a " +
            "JOIN FETCH s.grado g " +
            "WHERE m.estudiante.id = :estudianteId " +
            "AND a.id = :anioId " +
            "AND m.estado = :estado " +
            "ORDER BY m.id DESC")
    List<Matricula> buscarPorEstudianteYAnioId(
            @Param("estudianteId") Long estudianteId,
            @Param("anioId") Long anioId,
            @Param("estado") EstadoMatricula estado
    );

    /**
     * Busca las matrículas de un estudiante para un año académico (por su id)
     * que estén en cualquiera de los estados indicados. La más reciente primero.
     */
    @Query("SELECT m FROM Matricula m " +
            "JOIN FETCH m.seccion s " +
            "JOIN FETCH s.anioAcademico a " +
            "JOIN FETCH s.grado g " +
            "WHERE m.estudiante.id = :estudianteId " +
            "AND a.id = :anioId " +
            "AND m.estado IN :estados " +
            "ORDER BY m.id DESC")
    List<Matricula> buscarPorEstudianteYAnioIdYEstados(
            @Param("estudianteId") Long estudianteId,
            @Param("anioId") Long anioId,
            @Param("estados") Collection<EstadoMatricula> estados
    );

    /**
     * Listado administrativo de matrículas con filtros opcionales.
     */
    @Query(value = """
            SELECT m FROM Matricula m
            JOIN FETCH m.estudiante e
            JOIN FETCH m.seccion s
            JOIN FETCH s.grado g
            JOIN FETCH s.anioAcademico a
            WHERE (:anioId IS NULL OR a.id = :anioId)
              AND (:gradoId IS NULL OR g.id = :gradoId)
              AND (:seccionId IS NULL OR s.id = :seccionId)
              AND (:estado IS NULL OR m.estado = :estado)
              AND (:estudianteId IS NULL OR e.id = :estudianteId)
            """,
            countQuery = """
            SELECT COUNT(m) FROM Matricula m
            WHERE (:anioId IS NULL OR m.seccion.anioAcademico.id = :anioId)
              AND (:gradoId IS NULL OR m.seccion.grado.id = :gradoId)
              AND (:seccionId IS NULL OR m.seccion.id = :seccionId)
              AND (:estado IS NULL OR m.estado = :estado)
              AND (:estudianteId IS NULL OR m.estudiante.id = :estudianteId)
            """)
    Page<Matricula> listarMatriculas(
            @Param("anioId") Long anioId,
            @Param("gradoId") Long gradoId,
            @Param("seccionId") Long seccionId,
            @Param("estado") EstadoMatricula estado,
            @Param("estudianteId") Long estudianteId,
            Pageable pageable
    );

    /**
     * Matrículas en el estado indicado que no tienen un compromiso del tipo
     * solicitado en estado pagado. Se usa para la cancelación automática.
     */
    @Query("""
            SELECT m FROM Matricula m
            WHERE m.estado = :estadoMatricula
              AND m.seccion.anioAcademico.id = :anioId
              AND NOT EXISTS (
                  SELECT c FROM CompromisoPago c
                  WHERE c.matricula = m
                    AND c.tipoCompromiso = :tipoCompromiso
                    AND c.estado = :estadoCompromiso
              )
            """)
    List<Matricula> buscarSinCompromisoPagado(
            @Param("anioId") Long anioId,
            @Param("estadoMatricula") EstadoMatricula estadoMatricula,
            @Param("tipoCompromiso") TipoCompromiso tipoCompromiso,
            @Param("estadoCompromiso") EstadoCompromisoPago estadoCompromiso
    );

    /**
     * Matrículas pendientes de un año con los datos necesarios para emitir
     * compromisos en lote y notificar al apoderado.
     */
    @Query("""
            SELECT m FROM Matricula m
            JOIN FETCH m.estudiante e
            LEFT JOIN FETCH e.apoderado
            JOIN FETCH m.seccion s
            JOIN FETCH s.anioAcademico a
            WHERE a.id = :anioId
              AND m.estado = :estado
            ORDER BY m.id
            """)
    List<Matricula> buscarPendientesPorAnioConDetalle(
            @Param("anioId") Long anioId,
            @Param("estado") EstadoMatricula estado
    );

    /**
     * Matrículas de un año con un estado concreto y filtros opcionales de
     * grado/sección, con los datos necesarios para emitir compromisos en lote.
     */
    @Query("""
            SELECT m FROM Matricula m
            JOIN FETCH m.estudiante e
            LEFT JOIN FETCH e.apoderado
            JOIN FETCH m.seccion s
            JOIN FETCH s.grado g
            JOIN FETCH s.anioAcademico a
            WHERE a.id = :anioId
              AND m.estado = :estado
              AND (:gradoId IS NULL OR g.id = :gradoId)
              AND (:seccionId IS NULL OR s.id = :seccionId)
            ORDER BY m.id
            """)
    List<Matricula> buscarPorAnioYEstadoConDetalle(
            @Param("anioId") Long anioId,
            @Param("estado") EstadoMatricula estado,
            @Param("gradoId") Long gradoId,
            @Param("seccionId") Long seccionId
    );
}