package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SesionClaseRepositoryCustomImplTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final SesionClaseRepositoryCustomImpl repositorio = new SesionClaseRepositoryCustomImpl(jdbc);

    private static SesionClase sesion(long bloqueId, LocalDate fecha, EstadoSesion estado) {
        BloqueHorario bloque = new BloqueHorario();
        bloque.setId(bloqueId);
        SesionClase sesion = new SesionClase();
        sesion.setFecha(fecha);
        sesion.setHoraInicio(LocalDateTime.of(fecha, java.time.LocalTime.of(8, 0)));
        sesion.setHoraFin(LocalDateTime.of(fecha, java.time.LocalTime.of(9, 0)));
        sesion.setEstado(estado);
        sesion.setBloqueHorario(bloque);
        return sesion;
    }

    @Test
    void sinSesionesNoTocaLaBase() {
        repositorio.insertarEnLotes(List.of());

        verify(jdbc, never()).update(anyString(), org.mockito.ArgumentMatchers.<Object[]>any());
    }

    @Test
    void agrupaLasSesionesEnSentenciasDeVariasFilas() {
        List<SesionClase> sesiones = new ArrayList<>();
        LocalDate fecha = LocalDate.of(2026, 3, 2);
        for (int i = 0; i < 1200; i++) {
            sesiones.add(sesion(10L + i, fecha, EstadoSesion.PROGRAMADA));
        }

        repositorio.insertarEnLotes(sesiones);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, times(3)).update(sql.capture(), parametros.capture());

        // 500 + 500 + 200 filas, cinco parámetros por fila.
        assertEquals(List.of(500 * 5, 500 * 5, 200 * 5),
                parametros.getAllValues().stream().map(valores -> valores.length).toList());
        assertEquals(500, sql.getAllValues().get(0).split("\\(\\?, \\?, \\?, \\?, \\?\\)", -1).length - 1);
        assertEquals(200, sql.getAllValues().get(2).split("\\(\\?, \\?, \\?, \\?, \\?\\)", -1).length - 1);
    }

    @Test
    void guardaFechaHorasEstadoYBloqueDeCadaSesion() {
        repositorio.insertarEnLotes(List.of(sesion(7L, LocalDate.of(2026, 3, 2), EstadoSesion.OMITIDA)));

        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), parametros.capture());
        Object[] valores = parametros.getValue();

        assertEquals(java.sql.Date.valueOf("2026-03-02"), valores[0]);
        assertEquals(java.sql.Timestamp.valueOf("2026-03-02 08:00:00"), valores[1]);
        assertEquals(java.sql.Timestamp.valueOf("2026-03-02 09:00:00"), valores[2]);
        assertEquals("OMITIDA", valores[3]);
        assertEquals(7L, valores[4]);
    }
}
