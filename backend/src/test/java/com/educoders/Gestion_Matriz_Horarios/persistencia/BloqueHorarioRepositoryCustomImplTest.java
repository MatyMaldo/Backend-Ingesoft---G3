package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BloqueHorarioRepositoryCustomImplTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Captor private ArgumentCaptor<String> sql;
    @Captor private ArgumentCaptor<Object[]> parametros;

    private BloqueHorario bloque(long asignacionId, long aulaId, DiaSemana dia, int hora) {
        AsignacionCurso asignacion = new AsignacionCurso();
        asignacion.setId(asignacionId);
        Aula aula = new Aula();
        aula.setId(aulaId);
        BloqueHorario bloque = new BloqueHorario();
        bloque.setAsignacionCurso(asignacion);
        bloque.setAula(aula);
        bloque.setDiaSemana(dia);
        bloque.setHoraInicio(LocalTime.of(hora, 0));
        bloque.setHoraFin(LocalTime.of(hora, 45));
        return bloque;
    }

    private List<BloqueHorario> bloques(int cantidad) {
        List<BloqueHorario> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            lista.add(bloque(100L + i, 200L + i, DiaSemana.MARTES, 8 + (i % 5)));
        }
        return lista;
    }

    private BloqueHorarioRepositoryCustomImpl repositorio() {
        return new BloqueHorarioRepositoryCustomImpl(jdbcTemplate);
    }

    @Test
    void insertaTodosLosBloquesEnUnaSolaSentenciaSiCaben() {
        repositorio().insertarEnLotes(bloques(3));

        verify(jdbcTemplate, times(1)).update(sql.capture(), parametros.capture());
        assertEquals("INSERT INTO bloques_horario "
                        + "(asignacion_curso_id, aula_id, dia_semana, hora_inicio, hora_fin) VALUES "
                        + "(?, ?, ?, ?, ?), (?, ?, ?, ?, ?), (?, ?, ?, ?, ?)",
                sql.getValue());
        assertEquals(15, parametros.getValue().length);
    }

    @Test
    void enviaLosValoresDeCadaBloqueEnElOrdenDeLasColumnas() {
        repositorio().insertarEnLotes(List.of(
                bloque(11L, 22L, DiaSemana.MIERCOLES, 9),
                bloque(33L, 44L, DiaSemana.VIERNES, 12)));

        verify(jdbcTemplate).update(anyString(), parametros.capture());
        assertEquals(List.of(
                        11L, 22L, "MIERCOLES", Time.valueOf(LocalTime.of(9, 0)), Time.valueOf(LocalTime.of(9, 45)),
                        33L, 44L, "VIERNES", Time.valueOf(LocalTime.of(12, 0)), Time.valueOf(LocalTime.of(12, 45))),
                List.of(parametros.getValue()));
    }

    @Test
    void dividePorLotesDeQuinientasFilas() {
        repositorio().insertarEnLotes(bloques(1200));

        verify(jdbcTemplate, times(3)).update(sql.capture(), parametros.capture());
        List<Object[]> argumentos = parametros.getAllValues();
        assertEquals(2500, argumentos.get(0).length);
        assertEquals(2500, argumentos.get(1).length);
        assertEquals(1000, argumentos.get(2).length);
        // Cada lote conserva el orden: el primer bloque del segundo lote es el 501.
        assertEquals(600L, argumentos.get(1)[0]);
        // La última sentencia lleva solo las 200 filas restantes.
        assertEquals(200, sql.getAllValues().get(2).split("\\(\\?, \\?, \\?, \\?, \\?\\)", -1).length - 1);
    }

    @Test
    void noHaceNadaSinBloques() {
        repositorio().insertarEnLotes(List.of());
        repositorio().insertarEnLotes(null);

        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void laSentenciaTieneUnaFilaPorBloque() {
        String sentencia = BloqueHorarioRepositoryCustomImpl.sentencia(500);

        assertEquals(500, sentencia.split("\\(\\?, \\?, \\?, \\?, \\?\\)", -1).length - 1);
        assertTrue(sentencia.startsWith("INSERT INTO bloques_horario"));
    }
}
