package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.presentacion.dto.EstudianteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarEstudianteRequest;

/**
 * Define las operaciones relacionadas con la gestión de estudiantes.
 */
public interface EstudianteService {

    /**
     * Registra un nuevo estudiante en el sistema.
     *
     * @param solicitud datos necesarios para registrar al estudiante
     * @return datos del estudiante registrado
     */
    EstudianteResponse registrarEstudiante(RegistrarEstudianteRequest solicitud);
}