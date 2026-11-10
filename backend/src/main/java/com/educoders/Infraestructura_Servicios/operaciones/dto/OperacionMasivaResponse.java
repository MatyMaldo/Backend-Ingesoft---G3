package com.educoders.Infraestructura_Servicios.operaciones.dto;

import com.educoders.Infraestructura_Servicios.operaciones.EstadoOperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.TipoOperacionMasiva;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado público de una operación masiva. No expone el usuario que la lanzó.
 */
public record OperacionMasivaResponse(
        String jobId,
        TipoOperacionMasiva tipo,
        EstadoOperacionMasiva estado,
        int total,
        int procesados,
        int exitosos,
        int omitidos,
        int errores,
        int porcentaje,
        String fase,
        List<String> detalleErrores,
        LocalDateTime creadoEn,
        LocalDateTime iniciadoEn,
        LocalDateTime finalizadoEn
) {

    public static OperacionMasivaResponse desde(OperacionMasiva operacion) {
        int total = operacion.getTotal();
        int procesados = operacion.getProcesados();
        int porcentaje = total <= 0
                ? 0
                : Math.min(100, (int) Math.round(procesados * 100.0 / total));

        return new OperacionMasivaResponse(
                operacion.getJobId(),
                operacion.getTipo(),
                operacion.getEstado(),
                total,
                procesados,
                operacion.getExitosos(),
                operacion.getOmitidos(),
                operacion.getErrores(),
                porcentaje,
                operacion.getFase(),
                operacion.getDetalleErrores(),
                operacion.getCreadoEn(),
                operacion.getIniciadoEn(),
                operacion.getFinalizadoEn()
        );
    }
}
