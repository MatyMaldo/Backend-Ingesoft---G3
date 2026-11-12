package com.educoders.Infraestructura_Servicios.enums;

/**
 * Tipo de error del log de errores (HU-SAT-06): clasificación cerrada que asigna el sistema según el origen del
 * error y que nadie puede editar. La etiqueta es el texto que se guarda en la columna {@code tipo_error} y que ve
 * el administrador.
 */
public enum TipoError {
    /** Error técnico durante una operación del usuario. */
    EXCEPCION_NO_CONTROLADA("Excepción no controlada"),
    /** Falla de un proceso del sistema (cierre del periodo de matrícula, asignación automática de "Falta"...). */
    PROCESO_AUTOMATICO("Proceso automático"),
    /** Falla en el envío de un correo electrónico. */
    NOTIFICACION_CORREO("Notificación por correo");

    private final String etiqueta;

    TipoError(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * Resuelve el tipo a partir de su etiqueta guardada. Un valor desconocido (registros antiguos, anteriores a la
     * lista cerrada, como "ErrorInterno") se trata como excepción no controlada para no romper la consulta del log.
     */
    public static TipoError desdeEtiqueta(String etiqueta) {
        for (TipoError tipo : values()) {
            if (tipo.etiqueta.equalsIgnoreCase(etiqueta == null ? "" : etiqueta.trim())) {
                return tipo;
            }
        }
        return EXCEPCION_NO_CONTROLADA;
    }
}
