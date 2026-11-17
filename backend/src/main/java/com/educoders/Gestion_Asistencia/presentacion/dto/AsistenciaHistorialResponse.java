package com.educoders.Gestion_Asistencia.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record AsistenciaHistorialResponse(
        List<CursoItem> cursos,
        List<RegistroItem> registros,
        int pagina,
        int tamano,
        long totalRegistros,
        int totalPaginas,
        long totalPresentes,
        long totalTardanzas,
        long totalFaltas,
        long totalJustificadas,
        double porcentajeAsistencia,
        AnioItem anioAcademico
) {
    public AsistenciaHistorialResponse {
        cursos = List.copyOf(cursos);
        registros = List.copyOf(registros);
    }

    public record CursoItem(Long id, String nombre) {
    }

    /** Año académico de consulta al que corresponde el historial (puede ser {@code null}). */
    public record AnioItem(Long id, Integer anio, LocalDate fechaInicio, LocalDate fechaFin) {
    }

    public record RegistroItem(
            Long id,
            LocalDate fecha,
            Long cursoId,
            String cursoNombre,
            String seccionNombre,
            String gradoNombre,
            LocalTime horaInicio,
            LocalTime horaFin,
            EstadoAsistencia estado,
            String observaciones,
            /** Hora en que se registró la asistencia; solo se informa en el historial del docente (HU-ASI-08). */
            LocalDateTime fechaHoraRegistro
    ) {
    }
}
