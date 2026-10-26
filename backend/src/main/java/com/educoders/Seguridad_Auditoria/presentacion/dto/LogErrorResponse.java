package com.educoders.Seguridad_Auditoria.presentacion.dto;

import com.educoders.Seguridad_Auditoria.dominio.LogError;
import java.time.LocalDateTime;

public record LogErrorResponse(
        Long id,
        LocalDateTime fechaHora,
        Long usuarioId,
        String nombreUsuario,
        String tipoError,
        String mensaje,
        String detalle,
        String endpoint,
        String metodoHttp,
        Short codigoEstado
) {
    public static LogErrorResponse desdeEntidad(LogError entidad) {
        String nombreUsr = "Sistema";
        Long usrId = null;

        if (entidad.getUsuario() != null) {
            usrId = entidad.getUsuario().getId();
            nombreUsr = entidad.getUsuario().getNombre() + " " + entidad.getUsuario().getApellidoPaterno();
        }

        return new LogErrorResponse(
                entidad.getId(),
                entidad.getFechaHora(),
                usrId,
                nombreUsr,
                entidad.getTipoError(),
                entidad.getMensaje(),
                entidad.getDetalle(),
                entidad.getEndpoint(),
                entidad.getMetodoHttp(),
                entidad.getCodigoEstado()
        );
    }
}