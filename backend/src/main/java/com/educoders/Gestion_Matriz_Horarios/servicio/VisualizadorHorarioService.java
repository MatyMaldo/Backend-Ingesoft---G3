package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.HorarioSemanalResponse;

public interface VisualizadorHorarioService {
    HorarioSemanalResponse porSeccion(Long seccionId);
    HorarioSemanalResponse porDocente(Long docenteId);
    HorarioSemanalResponse miHorarioDocente();
    HorarioSemanalResponse miHorarioEstudiante();
    HorarioSemanalResponse porApoderado(Long estudianteId);
}
