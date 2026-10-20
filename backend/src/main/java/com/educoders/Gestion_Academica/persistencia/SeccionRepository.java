package com.educoders.Gestion_Academica.persistencia;

import com.educoders.Gestion_Academica.dominio.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeccionRepository extends JpaRepository<Seccion, Long> {
    boolean existsByGradoIdAndAnioAcademicoIdAndNombre(Long gradoId, Long anioAcademicoId, String nombre);
    boolean existsByAnioAcademicoIdAndTutorId(Long anioAcademicoId, Long tutorId);
    List<Seccion> findByGradoIdAndAnioAcademicoId(Long gradoId, Long anioAcademicoId);
}