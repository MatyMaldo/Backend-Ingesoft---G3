package com.educoders.Gestion_Matricula_Pagos.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MatriculaRepositoryCustomImplTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final MatriculaRepositoryCustomImpl repositorio = new MatriculaRepositoryCustomImpl(jdbc);

    private static Matricula matricula(long estudianteId, long seccionId, EstadoMatricula estado) {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(estudianteId);
        Seccion seccion = new Seccion();
        seccion.setId(seccionId);
        return Matricula.builder()
                .fechaAsignacion(LocalDate.of(2026, 11, 18))
                .estado(estado)
                .estudiante(estudiante)
                .seccion(seccion)
                .build();
    }

    @Test
    void sinMatriculasNoTocaLaBase() {
        repositorio.insertarEnLotes(List.of());

        verify(jdbc, never()).update(anyString(), org.mockito.ArgumentMatchers.<Object[]>any());
    }

    @Test
    void agrupaLasMatriculasEnSentenciasDeVariasFilas() {
        List<Matricula> matriculas = new ArrayList<>();
        for (int i = 0; i < 1200; i++) {
            matriculas.add(matricula(100L + i, 1L, EstadoMatricula.PENDIENTE));
        }

        repositorio.insertarEnLotes(matriculas);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, times(3)).update(sql.capture(), parametros.capture());

        // 500 + 500 + 200 filas, cuatro parámetros por fila.
        assertEquals(List.of(500 * 4, 500 * 4, 200 * 4),
                parametros.getAllValues().stream().map(valores -> valores.length).toList());
        assertEquals(500, sql.getAllValues().get(0).split("\\(\\?, \\?, \\?, \\?\\)", -1).length - 1);
        assertEquals(200, sql.getAllValues().get(2).split("\\(\\?, \\?, \\?, \\?\\)", -1).length - 1);
    }

    @Test
    void guardaFechaEstadoEstudianteYSeccionDeCadaMatricula() {
        repositorio.insertarEnLotes(List.of(
                matricula(7L, 3L, EstadoMatricula.PENDIENTE),
                matricula(8L, 4L, EstadoMatricula.CONFIRMADA)));

        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), parametros.capture());
        Object[] valores = parametros.getValue();

        assertEquals(java.sql.Date.valueOf("2026-11-18"), valores[0]);
        assertEquals("PENDIENTE", valores[1]);
        assertEquals(7L, valores[2]);
        assertEquals(3L, valores[3]);
        assertEquals("CONFIRMADA", valores[5]);
        assertEquals(8L, valores[6]);
        assertEquals(4L, valores[7]);
    }

    @Test
    void laSentenciaInsertaEnLasColumnasEnElOrdenDeLosParametros() {
        assertEquals("INSERT INTO matriculas (fecha_asignacion, estado, estudiante_id, seccion_id) VALUES "
                        + "(?, ?, ?, ?), (?, ?, ?, ?)",
                MatriculaRepositoryCustomImpl.sentencia(2));
    }
}
