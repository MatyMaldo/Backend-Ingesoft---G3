package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.presentacion.dto.DocenteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarDocenteRequest;

public interface DocenteService {

    /**
     * HU-SAT-01: Registra un docente con sus datos básicos y lo habilita
     * para iniciar sesión con el rol DOCENTE.
     */
    DocenteResponse registrarDocente(RegistrarDocenteRequest solicitud);
}