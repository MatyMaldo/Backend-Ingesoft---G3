package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.presentacion.dto.ApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.EstudianteApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarApoderadoRequest;

import java.util.List;

/**
 * Define las operaciones relacionadas con la gestión de apoderados.
 */
public interface ApoderadoService {

    /**
     * Registra un nuevo apoderado en el sistema.
     *
     * @param solicitud datos necesarios para registrar al apoderado
     * @return datos del apoderado registrado
     */
    ApoderadoResponse registrarApoderado(RegistrarApoderadoRequest solicitud);

    /**
     * Lista los estudiantes vinculados al apoderado autenticado.
     * Si se indica {@code anioId}, completa grado, sección y año con la
     * matrícula de ese año.
     */
    List<EstudianteApoderadoResponse> listarMisEstudiantes(Long anioId);
}