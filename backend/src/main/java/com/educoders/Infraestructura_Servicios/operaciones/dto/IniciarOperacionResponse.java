package com.educoders.Infraestructura_Servicios.operaciones.dto;

import com.educoders.Infraestructura_Servicios.operaciones.EstadoOperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.TipoOperacionMasiva;

import java.time.LocalDateTime;

/**
 * Respuesta inmediata al iniciar una operación masiva asíncrona.
 */
public record IniciarOperacionResponse(
        String jobId,
        TipoOperacionMasiva tipo,
        EstadoOperacionMasiva estado,
        LocalDateTime creadoEn
) {

    public static IniciarOperacionResponse desde(OperacionMasiva operacion) {
        return new IniciarOperacionResponse(
                operacion.getJobId(),
                operacion.getTipo(),
                operacion.getEstado(),
                operacion.getCreadoEn()
        );
    }
}
