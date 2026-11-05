
package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.NotasCompetenciasResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.RegistrarNotasCompetenciasRequest;
import com.educoders.Seguridad_Auditoria.dominio.Docente;

/**
 * Servicio de registro de calificaciones finales de competencias.
 * HU-EVAL-07.
 */
public interface NotasCompetenciasService {

    /**
     * Registra o actualiza las calificaciones de las competencias
     * de un estudiante en un curso y bimestre.
     */
    NotasCompetenciasResponse registrarNotas(
            RegistrarNotasCompetenciasRequest request,
            Docente docente
    );
}
