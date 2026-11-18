package com.educoders.Gestion_Matricula_Pagos.persistencia;

import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;

import java.util.List;

/**
 * Operaciones de {@link Matricula} que no se resuelven con Spring Data directamente.
 */
public interface MatriculaRepositoryCustom {

    /**
     * Inserta muchas matrículas con pocas sentencias (un {@code INSERT} de varias filas por lote) en lugar de una por
     * matrícula. {@code Matricula} usa {@code IDENTITY}, con lo que {@code saveAll} haría un viaje a la base por cada
     * matrícula.
     * <p>
     * Participa en la transacción en curso, así que se revierte junto con ella. No devuelve los ids generados ni deja
     * las matrículas en el contexto de persistencia: solo es para la carga masiva. Una restricción única violada
     * (por ejemplo, estudiante y sección repetidos) lanza {@code DataIntegrityViolationException}.
     *
     * @param matriculas matrículas con fecha de asignación, estado, estudiante y sección ya asignados
     */
    void insertarEnLotes(List<Matricula> matriculas);
}
