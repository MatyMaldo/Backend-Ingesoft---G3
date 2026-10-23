package com.educoders.Gestion_Matricula_Pagos.persistencia;

import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.Mes;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompromisoPagoRepository extends JpaRepository<CompromisoPago, Long> {

    /**
     * Verifica si existe un compromiso de un determinado tipo (ej. MATRICULA) para la matrícula dada.
     */
    boolean existsByMatriculaIdAndTipoCompromiso(Long matriculaId, TipoCompromiso tipoCompromiso);
    Optional<CompromisoPago> findByMatriculaIdAndTipoCompromiso(Long matriculaId, TipoCompromiso tipoCompromiso);

    /**
     * HU-GEMAP-05: Verifica si ya existe una pensión emitida para una matrícula en un mes determinado.
     */
    boolean existsByMatriculaIdAndTipoCompromisoAndMes(Long matriculaId, TipoCompromiso tipoCompromiso, Mes mes);
    Optional<CompromisoPago> findByMatriculaIdAndTipoCompromisoAndMes(Long matriculaId, TipoCompromiso tipoCompromiso, Mes mes);

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
}