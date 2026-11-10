package com.educoders.Infraestructura_Servicios.operaciones;

import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperacionMasivaServiceTest {

    private OperacionMasivaService serviceCon(Executor executor) {
        return new OperacionMasivaService(executor);
    }

    @Test
    void iniciarEjecutaTareaYCompleta() {
        OperacionMasivaService service = serviceCon(Runnable::run);

        OperacionMasiva operacion = service.iniciar(
                TipoOperacionMasiva.EMISION_MATRICULA_LOTE, 7L, null,
                progreso -> {
                    progreso.iniciar(2);
                    progreso.actualizar(2, 2, 0, 0);
                });

        assertEquals(EstadoOperacionMasiva.COMPLETADO, operacion.getEstado());
        assertEquals(2, operacion.getTotal());
        assertEquals(2, operacion.getProcesados());
        assertEquals(2, operacion.getExitosos());
        assertEquals(0, operacion.getErrores());
        assertEquals("FINALIZADO", operacion.getFase());
        assertSame(operacion, service.obtener(operacion.getJobId()));
    }

    @Test
    void erroresDejanEstadoCompletadoConErrores() {
        OperacionMasivaService service = serviceCon(Runnable::run);

        OperacionMasiva operacion = service.iniciar(
                TipoOperacionMasiva.EMISION_PENSION_LOTE, 7L, null,
                progreso -> {
                    progreso.iniciar(2);
                    progreso.registrarError("Matrícula 1: fallo");
                    progreso.actualizar(2, 1, 0, 1);
                });

        assertEquals(EstadoOperacionMasiva.COMPLETADO_CON_ERRORES, operacion.getEstado());
        assertEquals(1, operacion.getErrores());
        assertEquals(1, operacion.getDetalleErrores().size());
    }

    @Test
    void excepcionDejaEstadoFallido() {
        OperacionMasivaService service = serviceCon(Runnable::run);

        OperacionMasiva operacion = service.iniciar(
                TipoOperacionMasiva.CONFIRMAR_HORARIOS_LOTE, 7L, null,
                progreso -> {
                    throw new IllegalStateException("boom");
                });

        assertEquals(EstadoOperacionMasiva.FALLIDO, operacion.getEstado());
        assertEquals(1, operacion.getErrores());
        assertEquals("boom", operacion.getDetalleErrores().get(0));
    }

    @Test
    void idempotenciaDevuelveElMismoJobYEjecutaUnaVez() {
        OperacionMasivaService service = serviceCon(Runnable::run);
        AtomicInteger ejecuciones = new AtomicInteger();

        TareaOperacionMasiva tarea = progreso -> ejecuciones.incrementAndGet();

        OperacionMasiva primera = service.iniciar(
                TipoOperacionMasiva.GENERACION_SESIONES, 7L, "clave-1", tarea);
        OperacionMasiva segunda = service.iniciar(
                TipoOperacionMasiva.GENERACION_SESIONES, 7L, "clave-1", tarea);

        assertSame(primera, segunda);
        assertEquals(1, ejecuciones.get());
    }

    @Test
    void idempotenciaConOtroTipoRechaza() {
        OperacionMasivaService service = serviceCon(Runnable::run);

        service.iniciar(TipoOperacionMasiva.GENERACION_SESIONES, 7L, "clave-2",
                progreso -> { });

        assertThrows(ReglaNegocioException.class, () -> service.iniciar(
                TipoOperacionMasiva.EMISION_PENSION_LOTE, 7L, "clave-2",
                progreso -> { }));
    }

    @Test
    void obtenerJobInexistenteLanzaNoEncontrado() {
        OperacionMasivaService service = serviceCon(Runnable::run);
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtener("job-inexistente"));
    }

    @Test
    void executorLlenoLanzaOperacionRechazada() {
        Executor executor = command -> {
            throw new RejectedExecutionException("cola llena");
        };
        OperacionMasivaService service = serviceCon(executor);

        assertThrows(OperacionMasivaRechazadaException.class, () -> service.iniciar(
                TipoOperacionMasiva.EMISION_MATRICULA_LOTE, 7L, null,
                progreso -> { }));
    }

    @Test
    void claveVaciaNoAplicaIdempotencia() {
        OperacionMasivaService service = serviceCon(Runnable::run);

        OperacionMasiva primera = service.iniciar(
                TipoOperacionMasiva.GENERACION_SESIONES, 7L, "   ", progreso -> { });
        OperacionMasiva segunda = service.iniciar(
                TipoOperacionMasiva.GENERACION_SESIONES, 7L, "", progreso -> { });

        assertEquals(false, primera.getJobId().equals(segunda.getJobId()));
    }
}
