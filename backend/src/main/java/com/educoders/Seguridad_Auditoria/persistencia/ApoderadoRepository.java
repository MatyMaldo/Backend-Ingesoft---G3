package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {

    Optional<Apoderado> findByEmail(String email);
    Optional<Apoderado> findByDni(String dni);
}