package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.HorarioSemanalResponse;

/**
 * @deprecated Usar {@link SesionClaseService} para consultar sesiones.
 */
@Deprecated(since = "2026-11", forRemoval = false)
public interface VisualizadorHorarioService {
    HorarioSemanalResponse porSeccion(Long seccionId);
    HorarioSemanalResponse porDocente(Long docenteId);
    HorarioSemanalResponse miHorarioDocente();
    HorarioSemanalResponse miHorarioEstudiante();
    HorarioSemanalResponse porApoderado(Long estudianteId);
}
