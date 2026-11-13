package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.NotaResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.RegistrarNotaRequest;
import com.educoders.Seguridad_Auditoria.dominio.Docente;

import java.util.List;

public interface NotaService {
    NotaResponse registrarOActualizarNota(Long idEvaluacion, RegistrarNotaRequest request, Docente docenteLogueado);

    List<NotaResponse> listarNotasPorEvaluacion(Long idEvaluacion);
}