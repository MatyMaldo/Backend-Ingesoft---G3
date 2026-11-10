package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.DiaSemana;

import java.time.LocalTime;
import java.util.List;

public record PreviewCargaMasivaResponse(
        Resumen resumen,
        List<ArchivoPreview> archivos,
        List<BloquePreview> bloques
) {
    public PreviewCargaMasivaResponse {
        archivos = List.copyOf(archivos);
        bloques = List.copyOf(bloques);
    }

    public record Resumen(
            int archivos,
            int filas,
            int validas,
            int conError,
            int duplicadas,
            int conflictos
    ) {
    }

    public record ArchivoPreview(
            String nombre,
            int filas,
            int errores
    ) {
    }

    public record BloquePreview(
            String previewId,
            String archivo,
            int fila,
            Long asignacionCursoId,
            Long seccionId,
            String gradoNombre,
            String seccionNombre,
            Long cursoId,
            String cursoNombre,
            Long docenteId,
            String docenteNombre,
            Long aulaId,
            String aulaNombre,
            DiaSemana diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin,
            String estado,
            List<String> errores
    ) {
        public BloquePreview {
            errores = List.copyOf(errores);
        }
    }
}
