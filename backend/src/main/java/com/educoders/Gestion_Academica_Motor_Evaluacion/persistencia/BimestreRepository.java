package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Bimestre;
import com.educoders.Infraestructura_Servicios.enums.NombreBimestre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BimestreRepository extends JpaRepository<Bimestre, Long> {


    // Spring Data JPA lee el nombre del método y genera el SQL automáticamente.
    // Busca el primer BIM_1 ordenado por el año del AnioAcademico de forma descendente.
    Optional<Bimestre> findFirstByNombreOrderByAnioAcademicoAnioDesc(NombreBimestre nombre);

}
