package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ActualizarSeccionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearGradoRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearSeccionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.GradoResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.SeccionResponse;
import java.util.List;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.VacantesAnioResponse;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.GradoConSeccionesResponse;
/**
 * Define las operaciones de negocio para la gestión
 * de grados y secciones académicas.
 */
public interface GradoSeccionService {
    GradoResponse crearGrado(CrearGradoRequest request);
    /**
     * Obtiene la lista de grados académicos registrados.
     *
     * @return lista de grados académicos
     */
    List<GradoResponse> listarGrados();
    /**
     * Obtiene un grado académico mediante su identificador.
     *
     * @param id identificador del grado académico
     * @return información del grado encontrado
     */

    GradoResponse obtenerGradoPorId(Long id);
    /**
     * Registra una nueva sección académica.
     *
     * @param request datos de la sección que se desea registrar
     * @return información de la sección registrada
     */
    SeccionResponse crearSeccion(CrearSeccionRequest request);
    /**
     * Obtiene todas las secciones académicas registradas.
     *
     * @return lista de secciones académicas
     */
    List<SeccionResponse> listarSecciones();
    /**
     * Obtiene las secciones de un grado y año académico específicos.
     *
     * @param gradoId identificador del grado académico
     * @param anioId identificador del año académico
     * @return lista de secciones que cumplen los filtros
     */
    List<SeccionResponse> listarSeccionesPorGradoYAnio(Long gradoId, Long anioId);
    /**
     * Actualiza los datos de una sección académica existente.
     *
     * @param id identificador de la sección
     * @param request datos que se desean actualizar
     * @return información actualizada de la sección
     */
    SeccionResponse actualizarSeccion(Long id, ActualizarSeccionRequest request);

    VacantesAnioResponse consultarVacantesAnio(Long anioId);

    List<GradoConSeccionesResponse> listarGradosConSecciones(Long anioId);

}