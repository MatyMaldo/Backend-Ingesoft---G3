package com.educoders.Infraestructura_Servicios.operaciones;

import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Registro y despacho en memoria de operaciones masivas.
 * <p>
 * Los jobs viven solo mientras el servidor está arriba. No hay persistencia ni
 * recuperación tras un reinicio, decisión aceptada para el alcance del proyecto.
 */
@Service
public class OperacionMasivaService {

    private static final Logger log = LoggerFactory.getLogger(OperacionMasivaService.class);
    private static final long RETENCION_MS = 60L * 60L * 1000L;
    private static final DateTimeFormatter FORMATO_ID =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Executor executor;
    private final Map<String, OperacionMasiva> operaciones = new ConcurrentHashMap<>();
    private final Map<String, String> clavesIdempotencia = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong();

    public OperacionMasivaService(
            @Qualifier("operacionesMasivasExecutor") Executor executor) {
        this.executor = executor;
    }

    /**
     * Crea y despacha una operación masiva.
     *
     * @param tipo              tipo de operación
     * @param usuarioId         usuario que la inicia; se usa para auditoría
     * @param claveIdempotencia clave opcional para evitar jobs duplicados
     * @param tarea             trabajo a ejecutar en segundo plano
     * @return el job creado, o el existente si la clave ya fue usada con el mismo tipo
     */
    public OperacionMasiva iniciar(TipoOperacionMasiva tipo,
                                   Long usuarioId,
                                   String claveIdempotencia,
                                   TareaOperacionMasiva tarea) {
        if (tipo == null || tarea == null) {
            throw new ReglaNegocioException("La operación masiva es obligatoria.");
        }

        String clave = normalizarClave(claveIdempotencia);
        if (clave != null) {
            String jobPrevio = clavesIdempotencia.get(clave);
            if (jobPrevio != null) {
                return resolverExistente(jobPrevio, tipo);
            }
        }

        OperacionMasiva operacion = new OperacionMasiva(nuevoJobId(), tipo, usuarioId);
        operaciones.put(operacion.getJobId(), operacion);

        if (clave != null) {
            String jobPrevio = clavesIdempotencia.putIfAbsent(clave, operacion.getJobId());
            if (jobPrevio != null) {
                operaciones.remove(operacion.getJobId());
                return resolverExistente(jobPrevio, tipo);
            }
        }

        try {
            executor.execute(() -> ejecutar(operacion, tarea));
        } catch (RejectedExecutionException ex) {
            operaciones.remove(operacion.getJobId());
            if (clave != null) {
                clavesIdempotencia.remove(clave, operacion.getJobId());
            }
            log.warn("No se pudo encolar la operación {}: {}", tipo, ex.getMessage());
            throw new OperacionMasivaRechazadaException(
                    "El servidor no tiene capacidad para iniciar otra operación masiva. Intente nuevamente en unos minutos.");
        }

        return operacion;
    }

    public OperacionMasiva obtener(String jobId) {
        OperacionMasiva operacion = operaciones.get(jobId);
        if (operacion == null) {
            throw new RecursoNoEncontradoException(
                    "No se encontró la operación masiva con id " + jobId + ".");
        }
        return operacion;
    }

    private OperacionMasiva resolverExistente(String jobId, TipoOperacionMasiva tipo) {
        OperacionMasiva existente = operaciones.get(jobId);
        if (existente == null) {
            throw new RecursoNoEncontradoException(
                    "No se encontró la operación masiva con id " + jobId + ".");
        }
        if (existente.getTipo() != tipo) {
            throw new ReglaNegocioException(
                    "La clave de idempotencia ya fue usada por otro tipo de operación.");
        }
        return existente;
    }

    private void ejecutar(OperacionMasiva operacion, TareaOperacionMasiva tarea) {
        operacion.iniciar(0);
        ProgresoLote progreso = new ProgresoLote() {
            @Override
            public void iniciar(int total) {
                operacion.iniciar(total);
            }

            @Override
            public void actualizar(int procesados, int exitosos, int omitidos, int errores) {
                operacion.actualizar(procesados, exitosos, omitidos, errores);
            }

            @Override
            public void registrarError(String mensaje) {
                operacion.registrarError(mensaje);
            }

            @Override
            public void fase(String mensaje) {
                operacion.fase(mensaje);
            }

            @Override
            public void resultado(Object resultado) {
                operacion.resultado(resultado);
            }
        };

        try {
            tarea.ejecutar(progreso);
            operacion.completar();
        } catch (Exception ex) {
            log.error("La operación masiva {} falló", operacion.getJobId(), ex);
            String mensaje = ex.getMessage() != null
                    ? ex.getMessage()
                    : ex.getClass().getSimpleName();
            operacion.fallar(mensaje);
        }
    }

    /**
     * Elimina jobs terminados hace más de una hora para no acumular memoria.
     */
    @Scheduled(fixedDelay = 15L * 60L * 1000L, initialDelay = 60L * 1000L)
    public void limpiarOperacionesAntiguas() {
        LocalDateTime limite = LocalDateTime.now().minusNanos(RETENCION_MS * 1_000_000L);
        operaciones.values().stream()
                .filter(OperacionMasiva::estaTerminada)
                .filter(op -> op.getFinalizadoEn() != null
                        && op.getFinalizadoEn().isBefore(limite))
                .map(OperacionMasiva::getJobId)
                .toList()
                .forEach(jobId -> {
                    operaciones.remove(jobId);
                    clavesIdempotencia.entrySet().removeIf(entry -> jobId.equals(entry.getValue()));
                });
    }

    private String normalizarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            return null;
        }
        return clave.trim();
    }

    private String nuevoJobId() {
        return "job-" + LocalDateTime.now().format(FORMATO_ID)
                + "-" + secuencia.incrementAndGet();
    }
}
