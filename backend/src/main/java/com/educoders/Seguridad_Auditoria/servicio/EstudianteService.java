package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
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

    /**
     * Registra un estudiante dentro de una carga masiva, sin consultar la BD para validar unicidad
     * ni para buscar al apoderado.
     * <p>
     * El llamador debe haber verificado antes que el DNI, el correo y el username no existen y debe
     * entregar el apoderado ya cargado. Las restricciones UNIQUE de la BD siguen siendo la última barrera.
     *
     * @param solicitud datos del estudiante, ya validados
     * @param apoderado apoderado ya cargado al que se vincula el estudiante
     */
    void registrarEstudianteEnLote(RegistrarEstudianteRequest solicitud, Apoderado apoderado);
}