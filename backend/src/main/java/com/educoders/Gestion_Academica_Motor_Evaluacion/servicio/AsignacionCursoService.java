package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.*;
import java.util.List;

public interface AsignacionCursoService {
    AsignacionCursoResponse crear(CrearAsignacionCursoRequest request);
    List<AsignacionCursoResponse> listar(Long gradoId, Long seccionId, Long anioId, Long docenteId);
    AsignacionCursoResponse obtener(Long id);
    AsignacionCursoResponse actualizar(Long id, ActualizarAsignacionCursoRequest request);
    void eliminar(Long id);
}