package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import java.util.List;

/**
 * Resultado del preview de carga masiva de matrículas.
 * No persiste ningún dato.
 */
public record PreviewCargaMasivaMatriculasResponse(
        Resumen resumen,
        List<FilaPreview> filas,
        List<SeccionDisponible> secciones
) {

    public PreviewCargaMasivaMatriculasResponse {
        filas = filas == null ? List.of() : List.copyOf(filas);
        secciones = secciones == null ? List.of() : List.copyOf(secciones);
    }

    public record Resumen(
            int filas,
            int validas,
            int conError,
            int duplicadas,
            int conflictos
    ) {
    }

    public record FilaPreview(
            String previewId,
            String origen,
            int fila,
            Long estudianteId,
            String dni,
            String codigo,
            String nombreCompleto,
            Long anioAcademicoId,
            Long gradoId,
            String gradoNombre,
            Long seccionId,
            String seccionNombre,
            String estado,
            List<String> errores
    ) {
        public FilaPreview {
            errores = errores == null ? List.of() : List.copyOf(errores);
        }
    }

    public record SeccionDisponible(
            Long id,
            String nombre,
            Long gradoId,
            String gradoNombre,
            Long anioAcademicoId,
            Integer anioAcademico,
            Integer capacidadMaxima,
            long matriculados,
            long vacantes
    ) {
    }
}
