package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.AnioAcademicoResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.PeriodoMatriculaEstadoResponse;

import java.util.List;
import java.util.Optional;

public interface AnioAcademicoService {
    List<AnioAcademicoResponse> listarAniosAcademicos();
    Optional<AnioAcademicoResponse> obtenerAnioAcademicoConPeriodoMatriculaVigente();

    /**
     * Estado del periodo de matrícula del año indicado y las ventanas
     * Habilitadas para asignación, bloques y generación de sesiones.
     */
    PeriodoMatriculaEstadoResponse obtenerPeriodoMatricula(Long anioId);
}
