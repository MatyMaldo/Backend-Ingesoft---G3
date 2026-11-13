package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ContextoDocenteResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionResponse;
import com.educoders.Seguridad_Auditoria.dominio.Docente;

import java.util.List;
import java.util.Optional;

public interface EvaluacionService {

    /**
     * Registra una nueva evaluación en el sistema vinculada a una competencia.
     * Regla de negocio: HU-EVAL-01.
     */
    EvaluacionResponse crearEvaluacion(EvaluacionRequest request, Docente docenteLogueado);

    /**
     * Lista evaluaciones con filtros opcionales por asignación de curso, sección o curso.
     */
    List<EvaluacionResponse> listarEvaluaciones(Optional<Long> idAsignacionCurso, Optional<Long> idSeccion, Optional<Long> idCurso);

    /**
     * Obtiene el contexto del docente: asignaciones, año vigente, bimestres, secciones y competencias.
     */
    ContextoDocenteResponse obtenerContextoDocente(Docente docenteLogueado);
}
