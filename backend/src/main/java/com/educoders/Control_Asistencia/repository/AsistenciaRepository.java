package com.educoders.Control_Asistencia.repository;

import com.educoders.Control_Asistencia.entity.AsistenciaEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Proporciona acceso a los registros de asistencia almacenados en la base de datos.
 */
public interface AsistenciaRepository extends JpaRepository<AsistenciaEstudiante, Long> {

    @Query("SELECT a FROM AsistenciaEstudiante a ORDER BY a.id")
    List<AsistenciaEstudiante> listarAsistencias();

    default AsistenciaEstudiante guardarAsistencia(AsistenciaEstudiante asistencia) {
        return save(asistencia);
    }

    default long contarAsistencias() {
        return count();
    }
}
