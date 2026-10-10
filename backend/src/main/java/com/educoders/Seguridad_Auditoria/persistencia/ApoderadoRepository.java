package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {
}