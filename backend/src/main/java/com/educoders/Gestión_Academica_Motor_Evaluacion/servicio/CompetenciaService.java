package com.educoders.Gestión_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestión_Academica_Motor_Evaluacion.presentacion.dto.ActualizarCompetenciaRequest;
import com.educoders.Gestión_Academica_Motor_Evaluacion.presentacion.dto.CompetenciaResponse;
import com.educoders.Gestión_Academica_Motor_Evaluacion.presentacion.dto.CrearCompetenciaRequest;
import java.util.List;

public interface CompetenciaService {
    List<CompetenciaResponse> listarPorCurso(Long idCurso);
    CompetenciaResponse crear(Long idCurso, CrearCompetenciaRequest request);
    CompetenciaResponse actualizar(Long idCurso, Long idCompetencia, ActualizarCompetenciaRequest request);
    void eliminar(Long idCurso, Long idCompetencia);
}