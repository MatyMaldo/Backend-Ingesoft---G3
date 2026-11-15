package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.presentacion.dto.ApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CursoEstudianteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleCursoEstudianteResponse;
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

    /**
     * Lista los cursos en los que está matriculado un estudiante del apoderado
     * en el año académico vigente.
     *
     * @param estudianteId ID del estudiante a consultar
     * @return lista de cursos con información de grado, sección y año académico
     */
    List<CursoEstudianteResponse> listarCursosEstudiante(Long estudianteId);

    /**
     * Lista las evaluaciones con notas de un estudiante en un curso específico,
     * incluyendo información completa del curso, docente, bimestres y competencias.
     *
     * @param estudianteId ID del estudiante a consultar
     * @param cursoId ID del curso a consultar
     * @return detalle completo del curso con evaluaciones y notas del estudiante
     */
    DetalleCursoEstudianteResponse listarEvaluacionesEstudianteCurso(Long estudianteId, Long cursoId);
}