package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.AnioAcademicoResponse;

import java.util.List;

public interface AnioAcademicoService {
    List<AnioAcademicoResponse> listarAniosAcademicos();
}
