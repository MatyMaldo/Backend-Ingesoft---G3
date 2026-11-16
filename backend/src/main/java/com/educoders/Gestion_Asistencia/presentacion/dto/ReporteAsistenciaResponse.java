package com.educoders.Gestion_Asistencia.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ReporteAsistenciaResponse(
        Long id, LocalDate fechaEmision, LocalDate desde, LocalDate hasta,
        String docente, String estudiante, String grado, String seccion, String curso,
        String comentarios, List<Falta> faltas) {

    public record Falta(LocalDate fecha, LocalTime horaInicio,
                        EstadoAsistencia estado, String observaciones) {
    }

    public record VistaPrevia(List<Falta> faltas, boolean puedeGenerar, String mensaje) {
    }

    public record Listado(List<Resumen> reportes, String mensaje) {
    }

    public record Resumen(Long id, LocalDate fechaEmision, LocalDate desde, LocalDate hasta,
                          String docente, String grado, String seccion, String curso) {
    }
}
