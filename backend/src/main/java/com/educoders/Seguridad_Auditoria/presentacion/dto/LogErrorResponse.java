package com.educoders.Seguridad_Auditoria.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.Modulo;
import com.educoders.Infraestructura_Servicios.enums.TipoError;
import com.educoders.Seguridad_Auditoria.dominio.LogError;
import java.time.LocalDateTime;

public record LogErrorResponse(
        Long id,
        LocalDateTime fechaHora,
        Long usuarioId,
        String nombreUsuario,
        Modulo modulo,
        String moduloEtiqueta,
        String tipoError,
        String mensaje,
        String detalle,
        String endpoint,
        String metodoHttp,
        Short codigoEstado
) {
    public static final String USUARIO_SISTEMA = "Sistema";
    public static final String USUARIO_NO_DETERMINADO = "No determinado";

    /**
     * Usuario involucrado (HU-SAT-06): el usuario autenticado cuando se pudo determinar; "Sistema" en los procesos
     * automáticos; y "No determinado" cuando el error ocurre en un flujo sin sesión (sin usuario y sin ser un proceso
     * automático, por ejemplo "¿Olvidaste tu contraseña?").
     */
    public static LogErrorResponse desdeEntidad(LogError entidad) {
        String nombreUsr = entidad.getTipoError() == TipoError.PROCESO_AUTOMATICO
                ? USUARIO_SISTEMA
                : USUARIO_NO_DETERMINADO;
        Long usrId = null;

        if (entidad.getUsuario() != null) {
            usrId = entidad.getUsuario().getId();
            nombreUsr = entidad.getUsuario().getNombre() + " " + entidad.getUsuario().getApellidoPaterno();
        }

        Modulo modulo = entidad.getModulo();

        return new LogErrorResponse(
                entidad.getId(),
                entidad.getFechaHora(),
                usrId,
                nombreUsr,
                modulo,
                modulo != null ? modulo.getEtiqueta() : null,
                entidad.getTipoError() != null ? entidad.getTipoError().getEtiqueta() : null,
                entidad.getMensaje(),
                entidad.getDetalle(),
                entidad.getEndpoint(),
                entidad.getMetodoHttp(),
                entidad.getCodigoEstado()
        );
    }
}