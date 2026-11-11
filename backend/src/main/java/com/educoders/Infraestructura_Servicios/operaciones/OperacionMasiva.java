package com.educoders.Infraestructura_Servicios.operaciones;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro en memoria del estado de una operación masiva.
 * <p>
 * No se persiste: si el servidor se reinicia, los jobs se pierden. Es un
 * compromiso aceptado para el alcance actual del proyecto.
 */
public class OperacionMasiva {

    private static final int MAX_ERRORES_DETALLE = 100;

    private final String jobId;
    private final TipoOperacionMasiva tipo;
    private final Long usuarioId;
    private final LocalDateTime creadoEn;

    private EstadoOperacionMasiva estado = EstadoOperacionMasiva.EN_COLA;
    private int total;
    private int procesados;
    private int exitosos;
    private int omitidos;
    private int errores;
    private String fase;
    private final List<String> detalleErrores = new ArrayList<>();
    private Object resultado;
    private LocalDateTime iniciadoEn;
    private LocalDateTime finalizadoEn;

    public OperacionMasiva(String jobId, TipoOperacionMasiva tipo, Long usuarioId) {
        this.jobId = jobId;
        this.tipo = tipo;
        this.usuarioId = usuarioId;
        this.creadoEn = LocalDateTime.now();
    }

    public String getJobId() {
        return jobId;
    }

    public TipoOperacionMasiva getTipo() {
        return tipo;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public synchronized EstadoOperacionMasiva getEstado() {
        return estado;
    }

    public synchronized int getTotal() {
        return total;
    }

    public synchronized int getProcesados() {
        return procesados;
    }

    public synchronized int getExitosos() {
        return exitosos;
    }

    public synchronized int getOmitidos() {
        return omitidos;
    }

    public synchronized int getErrores() {
        return errores;
    }

    public synchronized String getFase() {
        return fase;
    }

    public synchronized List<String> getDetalleErrores() {
        return List.copyOf(detalleErrores);
    }

    public synchronized Object getResultado() {
        return resultado;
    }

    public synchronized void resultado(Object resultado) {
        this.resultado = resultado;
    }

    public synchronized LocalDateTime getIniciadoEn() {
        return iniciadoEn;
    }

    public synchronized LocalDateTime getFinalizadoEn() {
        return finalizadoEn;
    }

    public synchronized void iniciar(int total) {
        if (estado != EstadoOperacionMasiva.EN_COLA
                && estado != EstadoOperacionMasiva.EN_PROCESO) {
            return;
        }
        this.total = Math.max(total, 0);
        if (estado == EstadoOperacionMasiva.EN_COLA) {
            this.estado = EstadoOperacionMasiva.EN_PROCESO;
            this.iniciadoEn = LocalDateTime.now();
        }
    }

    public synchronized void actualizar(int procesados, int exitosos, int omitidos, int errores) {
        this.procesados = Math.max(procesados, 0);
        this.exitosos = Math.max(exitosos, 0);
        this.omitidos = Math.max(omitidos, 0);
        this.errores = Math.max(errores, 0);
    }

    public synchronized void registrarError(String mensaje) {
        errores++;
        if (detalleErrores.size() < MAX_ERRORES_DETALLE) {
            detalleErrores.add(mensaje);
        }
    }

    public synchronized void fase(String mensaje) {
        this.fase = mensaje;
    }

    public synchronized void completar() {
        if (estado == EstadoOperacionMasiva.CANCELADO) {
            return;
        }
        this.estado = errores > 0
                ? EstadoOperacionMasiva.COMPLETADO_CON_ERRORES
                : EstadoOperacionMasiva.COMPLETADO;
        this.finalizadoEn = LocalDateTime.now();
        this.fase = "FINALIZADO";
    }

    public synchronized void fallar(String mensaje) {
        if (estado == EstadoOperacionMasiva.CANCELADO) {
            return;
        }
        this.estado = EstadoOperacionMasiva.FALLIDO;
        this.fase = "FALLIDO";
        this.finalizadoEn = LocalDateTime.now();
        if (mensaje != null && !mensaje.isBlank()) {
            registrarError(mensaje);
        }
    }

    public synchronized void cancelar() {
        if (estado == EstadoOperacionMasiva.COMPLETADO
                || estado == EstadoOperacionMasiva.COMPLETADO_CON_ERRORES
                || estado == EstadoOperacionMasiva.FALLIDO) {
            return;
        }
        this.estado = EstadoOperacionMasiva.CANCELADO;
        this.fase = "CANCELADO";
        this.finalizadoEn = LocalDateTime.now();
    }

    public synchronized boolean estaTerminada() {
        return estado == EstadoOperacionMasiva.COMPLETADO
                || estado == EstadoOperacionMasiva.COMPLETADO_CON_ERRORES
                || estado == EstadoOperacionMasiva.FALLIDO
                || estado == EstadoOperacionMasiva.CANCELADO;
    }
}
