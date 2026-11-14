package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * Implementación con {@link JdbcTemplate} de {@link SesionClaseRepositoryCustom}. Spring Data la une al
 * repositorio por su nombre (<code>…RepositoryCustomImpl</code>).
 */
@Repository
public class SesionClaseRepositoryCustomImpl implements SesionClaseRepositoryCustom {

    /** Filas por sentencia: 500 × 5 parámetros queda muy por debajo del máximo de MySQL (65535). */
    static final int FILAS_POR_SENTENCIA = 500;

    private static final String INSERTAR = "INSERT INTO sesiones_clase "
            + "(fecha, hora_inicio, hora_fin, estado, bloque_horario_id) VALUES ";
    private static final String FILA = "(?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public SesionClaseRepositoryCustomImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    // Siempre dentro de la transacción de quien la llama: si no, cada lote se confirmaría por su cuenta.
    @Transactional(propagation = Propagation.MANDATORY)
    public void insertarEnLotes(List<SesionClase> sesiones) {
        if (sesiones == null || sesiones.isEmpty()) {
            return;
        }
        for (int desde = 0; desde < sesiones.size(); desde += FILAS_POR_SENTENCIA) {
            List<SesionClase> lote = sesiones.subList(desde, Math.min(desde + FILAS_POR_SENTENCIA, sesiones.size()));
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

    private static Object[] parametros(List<SesionClase> lote) {
        Object[] valores = new Object[lote.size() * 5];
        int i = 0;
        for (SesionClase sesion : lote) {
            valores[i++] = Date.valueOf(sesion.getFecha());
            valores[i++] = Timestamp.valueOf(sesion.getHoraInicio());
            valores[i++] = Timestamp.valueOf(sesion.getHoraFin());
            valores[i++] = sesion.getEstado().name();
            valores[i++] = sesion.getBloqueHorario().getId();
        }
        return valores;
    }
}
