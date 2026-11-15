
package com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.LibretaNotas;
import com.educoders.Infraestructura_Servicios.enums.EstadoLibreta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de libretas de notas.
 * Permite consultar libretas y prevenir duplicados.
 */
@Repository
public interface LibretaNotasRepository extends JpaRepository<LibretaNotas, Long> {

    /**
     * HU-EVAL-04/05: Consulta las libretas generadas
     * de un estudiante para un año académico.
     */
    List<LibretaNotas>
    findByEstudiante_IdAndBimestre_AnioAcademico_IdAndEstadoOrderByBimestre_FechaFinDesc(
            Long estudianteId,
            Long anioAcademicoId,
            EstadoLibreta estado
    );

    /**
     * HU-EVAL-07: Verifica si las notas están
     * bloqueadas por una libreta generada.
     */
    boolean existsByEstudianteIdAndBimestreIdAndEstado(
            Long idEstudiante,
            Long idBimestre,
            EstadoLibreta estado
    );

    /**
     * HU-EVAL-06: Verifica si existe una libreta
     * para un estudiante y bimestre determinados.
     */
    boolean existsByEstudianteIdAndBimestreId(
            Long idEstudiante,
            Long idBimestre
    );

    /**
     * HU-EVAL-06: Obtiene una libreta existente
     * para evitar generar registros duplicados.
     */
    Optional<LibretaNotas> findByEstudianteIdAndBimestreId(
            Long idEstudiante,
            Long idBimestre
    );
}
