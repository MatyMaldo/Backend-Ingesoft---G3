package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.persistencia.UsuarioLoteRepository.DatosApoderado;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioLoteRepository.DatosEstudiante;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioLoteRepository.NuevoUsuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioLoteRepositoryTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Captor private ArgumentCaptor<String> sql;
    @Captor private ArgumentCaptor<Object[]> parametros;

    private UsuarioLoteRepository repositorio() {
        return new UsuarioLoteRepository(jdbcTemplate);
    }

    private static NuevoUsuario usuario(String tipo, int n) {
        return new NuevoUsuario(tipo, "Nombre" + n, "Paterno", n % 2 == 0 ? null : "Materno", "7000000" + n % 10,
                "user" + n, "user" + n + "@x.com", "98100100" + n % 10);
    }

    /** El SELECT de ids devuelve una fila (id, correo) por cada correo recibido, con ids a partir de 1000 en ORDEN INVERSO. */
    private void simularLecturaDeIds() {
        doAnswer(invocacion -> {
            Object[] correos = invocacion.getArguments();
            RowCallbackHandler manejador = invocacion.getArgument(1);
            // El primer argumento variable empieza en la posición 2 (sql, handler, correos...).
            int cantidad = correos.length - 2;
            for (int i = cantidad - 1; i >= 0; i--) {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getLong(1)).thenReturn(1000L + Long.parseLong(((String) correos[2 + i]).replaceAll("\\D", "")));
                when(rs.getString(2)).thenReturn(((String) correos[2 + i]).toUpperCase());
                manejador.processRow(rs);
            }
            return null;
        }).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
    }

    @Test
    void insertaLosUsuariosEnUnaSolaSentenciaConSuDiscriminadorYActivos() {
        simularLecturaDeIds();

        repositorio().insertarUsuarios(List.of(usuario("DOCENTE", 1), usuario("APODERADO", 2)));

        verify(jdbcTemplate).update(sql.capture(), parametros.capture());
        assertEquals("INSERT INTO usuarios (tipo_usuario, nombre, apellido_paterno, apellido_materno, dni, username, "
                        + "email, telefono, fecha_creacion, activo) VALUES "
                        + "(?, ?, ?, ?, ?, ?, ?, ?, ?, 1), (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                sql.getValue());
        Object[] valores = parametros.getValue();
        assertEquals(18, valores.length);
        // Fila 1, en el orden de las columnas; la fecha de creación es la misma para todo el lote.
        assertEquals("DOCENTE", valores[0]);
        assertEquals("Nombre1", valores[1]);
        assertEquals("Paterno", valores[2]);
        assertEquals("Materno", valores[3]);
        assertEquals("user1", valores[5]);
        assertEquals("user1@x.com", valores[6]);
        assertTrue(valores[8] instanceof Timestamp);
        assertEquals(valores[8], valores[17]);
        assertEquals("APODERADO", valores[9]);
        assertEquals(null, valores[12]);
    }

    @Test
    void devuelveLosIdsEnElMismoOrdenQueLosUsuariosAunqueLaBdLosEntregueDesordenados() {
        simularLecturaDeIds();

        List<Long> ids = repositorio().insertarUsuarios(List.of(
                usuario("DOCENTE", 5), usuario("DOCENTE", 3), usuario("DOCENTE", 4)));

        // El id simulado es 1000 + el número del correo; el correo vuelve en mayúsculas y se empareja igual.
        assertEquals(List.of(1005L, 1003L, 1004L), ids);
    }

    @Test
    void sinUsuarios_noHaceNada() {
        assertEquals(List.of(), repositorio().insertarUsuarios(List.of()));
        repositorio().insertarDocentes(List.of());
        repositorio().insertarApoderados(List.of());
        repositorio().insertarEstudiantes(List.of());

        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void partePorLoteMuyGrandeEnVariasSentencias() {
        simularLecturaDeIds();
        List<NuevoUsuario> usuarios = new ArrayList<>();
        for (int i = 0; i < UsuarioLoteRepository.FILAS_POR_SENTENCIA + 1; i++) {
            usuarios.add(usuario("DOCENTE", i));
        }

        List<Long> ids = repositorio().insertarUsuarios(usuarios);

        verify(jdbcTemplate, times(2)).update(anyString(), parametros.capture());
        assertEquals(UsuarioLoteRepository.FILAS_POR_SENTENCIA * 9, parametros.getAllValues().get(0).length);
        assertEquals(9, parametros.getAllValues().get(1).length);
        assertEquals(usuarios.size(), ids.size());
    }

    @Test
    void siUnUsuarioInsertadoNoApareceAlLeerSusIds_fallaParaRevertirLaTransaccion() {
        // El SELECT no devuelve ninguna fila.
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> repositorio().insertarUsuarios(List.of(usuario("DOCENTE", 1))));

        assertTrue(error.getMessage().contains("user1@x.com"));
    }

    @Test
    void insertaDocentesApoderadosYEstudiantesConSusColumnasPropias() {
        repositorio().insertarDocentes(List.of(11L, 12L));
        repositorio().insertarApoderados(List.of(new DatosApoderado(21L, 40), new DatosApoderado(22L, null)));
        repositorio().insertarEstudiantes(List.of(
                new DatosEstudiante(31L, "EST001", LocalDate.of(2012, 5, 10), 21L)));

        verify(jdbcTemplate, times(3)).update(sql.capture(), parametros.capture());
        assertEquals("INSERT INTO docentes (id) VALUES (?), (?)", sql.getAllValues().get(0));
        assertEquals(List.of(11L, 12L), List.of(parametros.getAllValues().get(0)));

        assertEquals("INSERT INTO apoderados (id, edad) VALUES (?, ?), (?, ?)", sql.getAllValues().get(1));
        assertEquals(21L, parametros.getAllValues().get(1)[0]);
        assertEquals(40, parametros.getAllValues().get(1)[1]);
        assertEquals(null, parametros.getAllValues().get(1)[3]);

        assertEquals("INSERT INTO estudiantes (id, codigo, fecha_nacimiento, apoderado_id) VALUES (?, ?, ?, ?)",
                sql.getAllValues().get(2));
        Object[] estudiante = parametros.getAllValues().get(2);
        assertEquals(31L, estudiante[0]);
        assertEquals("EST001", estudiante[1]);
        assertEquals(Date.valueOf(LocalDate.of(2012, 5, 10)), estudiante[2]);
        assertEquals(21L, estudiante[3]);
    }
}
