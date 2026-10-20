package com.educoders.Gestion_Academica.persistencia;

import com.educoders.Gestion_Academica.dominio.Grado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradoRepository extends JpaRepository<Grado, Long> {
    boolean existsByNombre(String nombre);
    Optional<Grado> findByNombre(String nombre);
}