package com.educoders.Gestion_Matricula_Pagos.persistencia;

import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;

/**
 * Implementación con {@link JdbcTemplate} de {@link MatriculaRepositoryCustom}. Spring Data la une al repositorio por
 * su nombre (<code>…RepositoryCustomImpl</code>).
 */
@Repository
public class MatriculaRepositoryCustomImpl implements MatriculaRepositoryCustom {

    /** Filas por sentencia: 500 × 4 parámetros queda muy por debajo del máximo de MySQL (65535). */
    static final int FILAS_POR_SENTENCIA = 500;

    private static final int PARAMETROS_POR_FILA = 4;

    private static final String INSERTAR = "INSERT INTO matriculas "
            + "(fecha_asignacion, estado, estudiante_id, seccion_id) VALUES ";
    private static final String FILA = "(?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public MatriculaRepositoryCustomImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    // Siempre dentro de la transacción de quien la llama: si no, cada lote se confirmaría por su cuenta.
    @Transactional(propagation = Propagation.MANDATORY)
    public void insertarEnLotes(List<Matricula> matriculas) {
        if (matriculas == null || matriculas.isEmpty()) {
            return;
        }
        for (int desde = 0; desde < matriculas.size(); desde += FILAS_POR_SENTENCIA) {
            List<Matricula> lote = matriculas.subList(desde, Math.min(desde + FILAS_POR_SENTENCIA, matriculas.size()));
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

    private static Object[] parametros(List<Matricula> lote) {
        Object[] valores = new Object[lote.size() * PARAMETROS_POR_FILA];
        int i = 0;
        for (Matricula matricula : lote) {
            valores[i++] = Date.valueOf(matricula.getFechaAsignacion());
            valores[i++] = matricula.getEstado().name();
            valores[i++] = matricula.getEstudiante().getId();
            valores[i++] = matricula.getSeccion().getId();
        }
        return valores;
    }
}
