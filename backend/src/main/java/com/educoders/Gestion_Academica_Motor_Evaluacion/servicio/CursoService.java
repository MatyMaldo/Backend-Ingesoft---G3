package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ActualizarCursoRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearCursoRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CursoResponse;

import java.util.List;

public interface CursoService {
    List<CursoResponse> listarTodos();
    CursoResponse crear(CrearCursoRequest request);
    CursoResponse actualizar(Long idCurso, ActualizarCursoRequest request);
    void eliminar(Long idCurso);
}
