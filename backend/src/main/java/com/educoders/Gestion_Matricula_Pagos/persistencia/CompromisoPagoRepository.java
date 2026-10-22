package com.educoders.Gestion_Matricula_Pagos.persistencia;

import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompromisoPagoRepository extends JpaRepository<CompromisoPago, Long> {

    /**
     * Verifica si existe un compromiso de un determinado tipo (ej. MATRICULA) para la matrícula dada.
     */
    boolean existsByMatriculaIdAndTipoCompromiso(Long matriculaId, TipoCompromiso tipoCompromiso);

    Optional<CompromisoPago> findByMatriculaIdAndTipoCompromiso(Long matriculaId, TipoCompromiso tipoCompromiso);
}