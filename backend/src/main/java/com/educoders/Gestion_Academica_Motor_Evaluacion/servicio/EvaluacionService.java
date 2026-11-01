package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionResponse;
import com.educoders.Seguridad_Auditoria.dominio.Docente;

public interface EvaluacionService {

    /**
     * Registra una nueva evaluación en el sistema vinculada a una competencia.
     * Regla de negocio: HU-EVAL-01.
     */
    EvaluacionResponse crearEvaluacion(EvaluacionRequest request, Docente docenteLogueado);
}
