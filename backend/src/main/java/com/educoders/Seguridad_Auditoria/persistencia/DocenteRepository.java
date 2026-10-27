package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.dominio.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocenteRepository extends JpaRepository<Docente, Long> {
    List<Docente> findByActivoTrue();
}