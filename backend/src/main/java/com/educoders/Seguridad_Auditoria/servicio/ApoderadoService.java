package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.presentacion.dto.ApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarApoderadoRequest;

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
}