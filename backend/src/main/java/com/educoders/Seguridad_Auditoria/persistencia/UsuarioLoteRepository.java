package com.educoders.Seguridad_Auditoria.persistencia;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Inserta usuarios nuevos por lotes con {@link JdbcTemplate}, varias filas por sentencia, para las cargas masivas.
 * <p>
 * Con JPA cada usuario cuesta dos {@code INSERT} (la tabla {@code usuarios} y la de su rol, por la herencia JOINED)
 * y Hibernate no agrupa los inserts de entidades con id autogenerado. Aquí un lote entero son unas pocas sentencias.
 * <p>
 * Reproduce lo que hace el registro individual: la cuenta nace activa, con la fecha actual y SIN contraseña (el
 * usuario la define con el código de verificación que recibe por correo). Las restricciones UNIQUE de la BD siguen
 * siendo la última barrera: si alguna fila las viola, la sentencia falla y la transacción de quien llama decide.
 */
@Repository
public class UsuarioLoteRepository {

    /** Filas por sentencia: 500 × 9 parámetros queda muy por debajo del máximo de MySQL (65535). */
    static final int FILAS_POR_SENTENCIA = 500;

    private static final String INSERTAR_USUARIOS = "INSERT INTO usuarios (tipo_usuario, nombre, apellido_paterno, "
            + "apellido_materno, dni, username, email, telefono, fecha_creacion, activo) VALUES ";
    private static final String FILA_USUARIO = "(?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";

    private final JdbcTemplate jdbcTemplate;

    public UsuarioLoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Datos de un usuario nuevo. {@code tipo} es el valor del discriminador: DOCENTE, APODERADO o ESTUDIANTE. */
    public record NuevoUsuario(String tipo, String nombre, String apellidoPaterno, String apellidoMaterno, String dni,
                               String username, String email, String telefono) {
    }

    /** Datos propios de un apoderado ya insertado en {@code usuarios}. */
    public record DatosApoderado(long usuarioId, LocalDate fechaNacimiento) {
    }

    /** Datos propios de un estudiante ya insertado en {@code usuarios}. */
    public record DatosEstudiante(long usuarioId, String codigo, LocalDate fechaNacimiento, long apoderadoId) {
    }

    /**
     * Inserta las filas de {@code usuarios} y devuelve el id de cada una, en el mismo orden que la lista recibida.
     * Los ids se leen por correo (único) en vez de deducirlos de los autoincrementales: no depende de cómo reparta
     * MySQL los ids dentro de una sentencia.
     */
    // Siempre dentro de la transacción de quien la llama: si no, cada lote se confirmaría por su cuenta.
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> insertarUsuarios(List<NuevoUsuario> usuarios) {
        if (usuarios == null || usuarios.isEmpty()) {
            return List.of();
        }
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        for (int desde = 0; desde < usuarios.size(); desde += FILAS_POR_SENTENCIA) {
            List<NuevoUsuario> lote = usuarios.subList(desde, Math.min(desde + FILAS_POR_SENTENCIA, usuarios.size()));
            jdbcTemplate.update(sentencia(INSERTAR_USUARIOS, FILA_USUARIO, lote.size()), parametrosUsuarios(lote, ahora));
        }
        return idsPorCorreo(usuarios);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void insertarDocentes(List<Long> usuarioIds) {
        insertarPorLotes("INSERT INTO docentes (id) VALUES ", "(?)", usuarioIds, 1,
                id -> new Object[]{id});
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void insertarApoderados(List<DatosApoderado> apoderados) {
        insertarPorLotes("INSERT INTO apoderados (id, fecha_nacimiento) VALUES ", "(?, ?)", apoderados, 2,
                a -> new Object[]{a.usuarioId(),
                        a.fechaNacimiento() == null ? null : Date.valueOf(a.fechaNacimiento())});
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void insertarEstudiantes(List<DatosEstudiante> estudiantes) {
        insertarPorLotes("INSERT INTO estudiantes (id, codigo, fecha_nacimiento, apoderado_id) VALUES ",
                "(?, ?, ?, ?)", estudiantes, 4,
                e -> new Object[]{e.usuarioId(), e.codigo(), Date.valueOf(e.fechaNacimiento()), e.apoderadoId()});
    }

    private <T> void insertarPorLotes(String insertar, String fila, List<T> elementos, int columnas,
                                      java.util.function.Function<T, Object[]> aParametros) {
        if (elementos == null || elementos.isEmpty()) {
            return;
        }
        for (int desde = 0; desde < elementos.size(); desde += FILAS_POR_SENTENCIA) {
            List<T> lote = elementos.subList(desde, Math.min(desde + FILAS_POR_SENTENCIA, elementos.size()));
            Object[] valores = new Object[lote.size() * columnas];
            int i = 0;
            for (T elemento : lote) {
                for (Object valor : aParametros.apply(elemento)) {
                    valores[i++] = valor;
                }
            }
            jdbcTemplate.update(sentencia(insertar, fila, lote.size()), valores);
        }
    }

    static String sentencia(String insertar, String fila, int filas) {
        StringBuilder sql = new StringBuilder(insertar);
        for (int i = 0; i < filas; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(fila);
        }
        return sql.toString();
    }

    private static Object[] parametrosUsuarios(List<NuevoUsuario> lote, Timestamp ahora) {
        Object[] valores = new Object[lote.size() * 9];
        int i = 0;
        for (NuevoUsuario u : lote) {
            valores[i++] = u.tipo();
            valores[i++] = u.nombre();
            valores[i++] = u.apellidoPaterno();
            valores[i++] = u.apellidoMaterno();
            valores[i++] = u.dni();
            valores[i++] = u.username();
            valores[i++] = u.email();
            valores[i++] = u.telefono();
            valores[i++] = ahora;
        }
        return valores;
    }

    /** Lee los ids recién creados (de a {@link #FILAS_POR_SENTENCIA} correos) y los ordena como la lista recibida. */
    private List<Long> idsPorCorreo(List<NuevoUsuario> usuarios) {
        Map<String, Long> ids = new HashMap<>();
        for (int desde = 0; desde < usuarios.size(); desde += FILAS_POR_SENTENCIA) {
            List<NuevoUsuario> lote = usuarios.subList(desde, Math.min(desde + FILAS_POR_SENTENCIA, usuarios.size()));
            String marcadores = String.join(", ", java.util.Collections.nCopies(lote.size(), "?"));
            Object[] correos = lote.stream().map(NuevoUsuario::email).toArray();
            jdbcTemplate.query("SELECT id, email FROM usuarios WHERE email IN (" + marcadores + ")",
                    rs -> {
                        ids.put(rs.getString(2).toLowerCase(Locale.ROOT), rs.getLong(1));
                    }, correos);
        }
        List<Long> resultado = new ArrayList<>(usuarios.size());
        for (NuevoUsuario usuario : usuarios) {
            Long id = ids.get(usuario.email().toLowerCase(Locale.ROOT));
            if (id == null) {
                throw new IllegalStateException("No se encontró el usuario recién insertado: " + usuario.email());
            }
            resultado.add(id);
        }
        return resultado;
    }
}
