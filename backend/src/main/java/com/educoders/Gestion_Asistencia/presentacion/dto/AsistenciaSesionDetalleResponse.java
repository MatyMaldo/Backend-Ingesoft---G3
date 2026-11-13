package com.educoders.Gestion_Asistencia.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AsistenciaSesionDetalleResponse(
        Long sesionId,
        Long cursoId,
        String cursoNombre,
        Long seccionId,
        String seccionNombre,
        String gradoNombre,
        Long docenteId,
        String docenteNombre,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        EstadoSesion estadoSesion,
        AsistenciaDocenteResponse asistenciaDocente,
        List<AsistenciaEstudianteDetalleResponse> estudiantes,
        boolean puedeRegistrarDocente,
        boolean puedeRegistrarEstudiantes,
        String motivoDocente,
        String motivoEstudiantes
) {
    public AsistenciaSesionDetalleResponse {
        estudiantes = List.copyOf(estudiantes);
    }
}
