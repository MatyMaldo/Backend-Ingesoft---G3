package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.RevisionNotasResponse;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;

public interface RevisionNotasService {
    RevisionNotasResponse obtenerNotasEstudiante(Long idEstudiante, Long idCurso, Usuario usuarioAutenticado);
}
