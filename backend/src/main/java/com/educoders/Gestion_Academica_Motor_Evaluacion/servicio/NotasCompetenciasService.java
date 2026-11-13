
package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.NotaCompetencia;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.NotaCompetenciaResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.NotasCompetenciasResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.RegistrarNotasCompetenciasRequest;
import com.educoders.Seguridad_Auditoria.dominio.Docente;

import java.util.List;
import java.util.Optional;

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

    /**
     * Lista notas de competencias con filtros opcionales.
     */
    List<NotaCompetenciaResponse> listarNotasCompetencias(
            Optional<Long> idEstudiante,
            Optional<Long> idBimestre,
            Optional<Long> idCurso
    );
}
