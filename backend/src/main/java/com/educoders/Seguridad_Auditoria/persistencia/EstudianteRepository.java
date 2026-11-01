package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    boolean existsByCodigo(String codigo);
    boolean existsByIdAndApoderadoId(Long estudianteId, Long apoderadoId);

    Optional<Estudiante> findByEmail(String email);
    Optional<Estudiante> findByDni(String dni);
}