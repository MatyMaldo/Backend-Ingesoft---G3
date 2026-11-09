package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

/**
 * Cantidad de matrículas por estado para los filtros vigentes del listado administrativo (año, grado, sección y
 * búsqueda; el estado no filtra, porque justamente se cuenta por estado).
 */
public record MatriculasPorEstadoResponse(
        long total,
        long pendientes,
        long confirmadas,
        long canceladas
) {
}
