package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarMatriculaRequest;

public interface MatriculaService {

    /**
     * HU-GEMAP-03: Registra la matrícula vinculando al estudiante con la sección elegida.
     */
    MatriculaResponse registrarMatricula(RegistrarMatriculaRequest request);
}