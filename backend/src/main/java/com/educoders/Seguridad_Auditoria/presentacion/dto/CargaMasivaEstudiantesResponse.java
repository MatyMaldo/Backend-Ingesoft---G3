
package com.educoders.Seguridad_Auditoria.presentacion.dto;

/**
 * Resultado de la carga masiva de estudiantes mediante CSV.
 * HU-GEMAP-02: registro masivo de estudiantes.
 */
public record CargaMasivaEstudiantesResponse(
        int estudiantesRegistrados,
        String mensaje
) {
}
