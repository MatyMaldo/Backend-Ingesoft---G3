package com.educoders.Gestion_Matricula_Pagos.persistencia;

import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    /**
     * Cuenta cuántas matrículas en los estados especificados ocupan capacidad en la sección.
     */
    long countBySeccionIdAndEstadoIn(Long seccionId, Collection<EstadoMatricula> estados);

    /**
     * Verifica si el estudiante ya cuenta con matrícula previa en los estados indicados
     * dentro del año académico al que pertenece la sección.
     */
    boolean existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoIn(
            Long estudianteId,
            Long anioAcademicoId,
            Collection<EstadoMatricula> estados
    );

    List<Matricula> findBySeccionId(Long seccionId);
}