package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Bimestre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface BimestreRepository extends JpaRepository<Bimestre, Long> {

    // Obtiene el primer bimestre del año académico más reciente
    @Query("SELECT b FROM Bimestre b WHERE b.nombre = 'BIM_1' ORDER BY b.anioAcademico.anio DESC LIMIT 1")
    Optional<Bimestre> findPrimerBimestreDelAnioMasReciente();
}
