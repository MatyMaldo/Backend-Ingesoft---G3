package com.educoders.Infraestructura_Servicios.operaciones;

/**
 * Callback opcional para reportar el avance de un lote.
 * <p>
 * Los procesos síncronos pueden pasar {@code null}; los jobs asíncronos usan una
 * implementación que actualiza el registro en memoria de la operación.
 */
public interface ProgresoLote {

    /** Marca el inicio de la operación e indica el total de elementos a procesar. */
    void iniciar(int total);

    /** Actualiza los contadores acumulados del lote. */
    void actualizar(int procesados, int exitosos, int omitidos, int errores);

    /** Registra un error puntual sin detener el lote. */
    void registrarError(String mensaje);

    /** Informa la fase actual (por ejemplo VALIDANDO o GUARDANDO). */
    void fase(String mensaje);

    /**
     * Publica el resultado final detallado de la operación (por ejemplo, el desenlace de cada fila). Se llama una vez,
     * al terminar. Por omisión no hace nada: solo lo usan las operaciones que devuelven un detalle.
     */
    default void resultado(Object resultado) {
    }
}
