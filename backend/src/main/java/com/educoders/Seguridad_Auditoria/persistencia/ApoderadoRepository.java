package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {

    Optional<Apoderado> findByEmail(String email);
    Optional<Apoderado> findByDni(String dni);

    /** Apoderados activos con alguno de los DNI dados (las cargas masivas vinculan estudiantes por el DNI del apoderado). */
    @Query("SELECT a FROM Apoderado a WHERE a.dni IN :dnis AND a.activo = true")
    List<Apoderado> findActivosPorDni(@Param("dnis") Collection<String> dnis);
}