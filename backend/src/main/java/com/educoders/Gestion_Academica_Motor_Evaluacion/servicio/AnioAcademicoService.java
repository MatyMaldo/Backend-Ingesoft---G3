package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.AnioAcademicoResponse;

import java.util.List;
import java.util.Optional;

public interface AnioAcademicoService {
    List<AnioAcademicoResponse> listarAniosAcademicos();
    Optional<AnioAcademicoResponse> obtenerAnioAcademicoConPeriodoMatriculaVigente();
}
