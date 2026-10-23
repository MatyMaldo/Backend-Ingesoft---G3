package com.educoders.Gestion_Matricula_Pagos.persistencia;

import com.educoders.Gestion_Matricula_Pagos.dominio.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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

}