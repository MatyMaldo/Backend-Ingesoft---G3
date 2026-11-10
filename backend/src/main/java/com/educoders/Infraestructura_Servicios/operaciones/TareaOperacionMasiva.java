package com.educoders.Infraestructura_Servicios.operaciones;

/**
 * Tarea que ejecuta un job de operación masiva.
 * <p>
 * Recibe el callback de progreso para publicar el avance sin conocer el
 * mecanismo de almacenamiento del job.
 */
@FunctionalInterface
public interface TareaOperacionMasiva {

    void ejecutar(ProgresoLote progreso) throws Exception;
}
