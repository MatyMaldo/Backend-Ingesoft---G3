package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.util.List;

/**
 * Implementación con {@link JdbcTemplate} de {@link BloqueHorarioRepositoryCustom}. Spring Data la une al
 * repositorio por su nombre (<code>…RepositoryCustomImpl</code>).
 */
@Repository
public class BloqueHorarioRepositoryCustomImpl implements BloqueHorarioRepositoryCustom {

    /** Filas por sentencia: 500 × 5 parámetros queda muy por debajo del máximo de MySQL (65535). */
    static final int FILAS_POR_SENTENCIA = 500;

    private static final String INSERTAR = "INSERT INTO bloques_horario "
            + "(asignacion_curso_id, aula_id, dia_semana, hora_inicio, hora_fin) VALUES ";
    private static final String FILA = "(?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public BloqueHorarioRepositoryCustomImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    // Siempre dentro de la transacción de quien la llama: si no, cada lote se confirmaría por su cuenta.
    @Transactional(propagation = Propagation.MANDATORY)
    public void insertarEnLotes(List<BloqueHorario> bloques) {
        if (bloques == null || bloques.isEmpty()) {
            return;
        }
        for (int desde = 0; desde < bloques.size(); desde += FILAS_POR_SENTENCIA) {
            List<BloqueHorario> lote = bloques.subList(desde, Math.min(desde + FILAS_POR_SENTENCIA, bloques.size()));
            jdbcTemplate.update(sentencia(lote.size()), parametros(lote));
        }
    }

    static String sentencia(int filas) {
        StringBuilder sql = new StringBuilder(INSERTAR);
        for (int i = 0; i < filas; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(FILA);
        }
        return sql.toString();
    }

    private static Object[] parametros(List<BloqueHorario> lote) {
        Object[] valores = new Object[lote.size() * 5];
        int i = 0;
        for (BloqueHorario bloque : lote) {
            valores[i++] = bloque.getAsignacionCurso().getId();
            valores[i++] = bloque.getAula().getId();
            valores[i++] = bloque.getDiaSemana().name();
            valores[i++] = Time.valueOf(bloque.getHoraInicio());
            valores[i++] = Time.valueOf(bloque.getHoraFin());
        }
        return valores;
    }
}
