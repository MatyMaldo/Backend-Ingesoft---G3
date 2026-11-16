package com.educoders.Gestion_Asistencia.persistencia;

import com.educoders.Gestion_Asistencia.dominio.ReporteAsistencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReporteAsistenciaRepository extends JpaRepository<ReporteAsistencia, Long> {

    @EntityGraph(attributePaths = {"docente", "estudiante", "faltas.asistencia.sesionClase.bloqueHorario.asignacionCurso"})
    List<ReporteAsistencia> findByEstudianteIdOrderByFechaEmisionDescIdDesc(Long estudianteId);

    @EntityGraph(attributePaths = {"docente", "estudiante", "faltas.asistencia.sesionClase.bloqueHorario.asignacionCurso"})
    Optional<ReporteAsistencia> findByIdAndEstudianteId(Long id, Long estudianteId);
}
