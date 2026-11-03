package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    boolean existsByNombre(String nombre);
    Optional<Aula> findByNombre(String nombre);
}