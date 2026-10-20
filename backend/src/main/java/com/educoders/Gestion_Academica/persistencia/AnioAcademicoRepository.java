package com.educoders.Gestion_Academica.persistencia;

import com.educoders.Gestion_Academica.dominio.AnioAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnioAcademicoRepository extends JpaRepository<AnioAcademico, Long> {
}