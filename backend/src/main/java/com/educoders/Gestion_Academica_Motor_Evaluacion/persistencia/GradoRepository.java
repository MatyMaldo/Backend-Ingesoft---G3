package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradoRepository extends JpaRepository<Grado, Long> {
    boolean existsByNombre(String nombre);
    Optional<Grado> findByNombre(String nombre);
}