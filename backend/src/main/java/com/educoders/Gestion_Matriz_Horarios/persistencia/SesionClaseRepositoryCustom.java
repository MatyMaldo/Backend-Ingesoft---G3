package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;

import java.util.List;

/**
 * Operaciones de {@link SesionClase} que no se resuelven con Spring Data directamente.
 */
public interface SesionClaseRepositoryCustom {

    /**
     * Inserta muchas sesiones con pocas sentencias (un {@code INSERT} de varias filas por lote) en lugar de una por
     * sesión. {@code SesionClase} usa {@code IDENTITY}, con lo que {@code saveAll} haría un viaje a la base por
     * cada sesión.
     * <p>
     * Participa en la transacción en curso, así que se revierte junto con ella. No devuelve los ids generados ni
     * deja las sesiones en el contexto de persistencia: solo es para la generación masiva.
     *
     * @param sesiones sesiones con fecha, horas, estado y bloque ya asignados
     */
    void insertarEnLotes(List<SesionClase> sesiones);
}
