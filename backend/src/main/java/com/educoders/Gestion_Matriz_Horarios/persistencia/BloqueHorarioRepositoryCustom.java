package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;

import java.util.List;

/**
 * Operaciones de {@link BloqueHorario} que no se resuelven con Spring Data directamente.
 */
public interface BloqueHorarioRepositoryCustom {

    /**
     * Inserta muchos bloques con pocas sentencias (un {@code INSERT} de varias filas por lote) en lugar de
     * uno por bloque. {@code BloqueHorario} usa {@code IDENTITY}, con lo que {@code saveAll} haría un viaje a la
     * base por cada bloque.
     * <p>
     * Participa en la transacción en curso, así que se revierte junto con ella. No devuelve los ids generados ni
     * deja los bloques en el contexto de persistencia: solo es para cargas masivas que no los vuelven a usar.
     *
     * @param bloques bloques con día, horas, aula y asignación de curso ya asignados
     */
    void insertarEnLotes(List<BloqueHorario> bloques);
}
