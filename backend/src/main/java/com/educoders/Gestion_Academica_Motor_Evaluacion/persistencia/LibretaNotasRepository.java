
package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.LibretaNotas;
import com.educoders.Infraestructura_Servicios.enums.EstadoLibreta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibretaNotasRepository extends JpaRepository<LibretaNotas, Long> {

    // Consultar libretas de un estudiante por año académico
    // y estado, ordenadas desde el bimestre más reciente.
    List<LibretaNotas> findByEstudiante_IdAndBimestre_AnioAcademico_IdAndEstadoOrderByBimestre_FechaFinDesc(
            Long estudianteId,
            Long anioAcademicoId,
            EstadoLibreta estado
    );



    /**
     * Comprueba si la libreta de un estudiante
     * ya fue generada para un bimestre.
     */
    boolean existsByEstudianteIdAndBimestreIdAndEstado(
            Long idEstudiante,
            Long idBimestre,
            EstadoLibreta estado
    );

}
