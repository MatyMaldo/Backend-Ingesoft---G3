package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ConfirmarCargaMasivaRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ConfirmarCargaMasivaResponse;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.operaciones.ProgresoLote;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Medición manual (no corre en {@code mvn test}) de {@code confirmar} de la carga masiva de horarios.
 * <p>
 * Cada ejecución corre dentro de UNA transacción que SIEMPRE se revierte: no queda ningún bloque guardado.
 * La auditoría se reemplaza por un mock porque se registra con {@code REQUIRES_NEW} (se confirmaría aunque
 * los bloques se revirtieran). Efecto colateral conocido: el contador AUTO_INCREMENT de {@code bloques_horario}
 * avanza (ids "quemados"). Se activa con {@code -Dbench.confirmar=true}.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@EnabledIfSystemProperty(named = "bench.confirmar", matches = "true")
class BenchConfirmarHorariosTest {

    @Autowired HorarioCargaMasivaService servicio;
    @Autowired EntityManagerFactory emf;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;
    @MockBean AuditoriaService auditoriaService;

    private record Asig(long id, long seccionId, long docenteId) {
    }

    private record Intervalo(LocalTime inicio, LocalTime fin) {
    }

    /** Registra el instante de cada fase para repartir el tiempo entre validar, guardar y finalizar. */
    private static final class Marcas implements ProgresoLote {
        final List<String> nombres = new ArrayList<>();
        final List<Long> instantes = new ArrayList<>();

        @Override
        public void iniciar(int total) {
            marcar("INICIO");
        }

        @Override
        public void actualizar(int procesados, int exitosos, int omitidos, int errores) {
        }

        @Override
        public void registrarError(String mensaje) {
        }

        @Override
        public void fase(String mensaje) {
            marcar(mensaje);
        }

        private void marcar(String nombre) {
            nombres.add(nombre);
            instantes.add(System.nanoTime());
        }
    }

    @Test
    void medirConfirmar() {
        Long anioId = jdbc.queryForObject(
                "SELECT anio_academico_id FROM periodos_matricula "
                        + "WHERE CURDATE() BETWEEN fecha_inicio AND fecha_fin LIMIT 1", Long.class);

        List<Asig> asignaciones = jdbc.query("""
                SELECT ac.id, ac.seccion_id, ac.docente_id FROM asignaciones_curso ac
                JOIN secciones s ON s.id = ac.seccion_id WHERE s.anio_academico_id = ? ORDER BY ac.id
                """, (rs, i) -> new Asig(rs.getLong(1), rs.getLong(2), rs.getLong(3)), anioId);
        List<Asig> elegidas = mayorConjuntoSinChoques(asignaciones);
        List<Long> aulas = jdbc.queryForList("SELECT id FROM aulas ORDER BY id", Long.class);

        Map<String, List<Intervalo>> existentes = new HashMap<>();
        jdbc.query("""
                SELECT b.dia_semana, b.hora_inicio, b.hora_fin FROM bloques_horario b
                JOIN asignaciones_curso ac ON ac.id = b.asignacion_curso_id
                JOIN secciones s ON s.id = ac.seccion_id WHERE s.anio_academico_id = ?
                """, rs -> {
            existentes.computeIfAbsent(rs.getString(1), k -> new ArrayList<>())
                    .add(new Intervalo(rs.getTime(2).toLocalTime(), rs.getTime(3).toLocalTime()));
        }, anioId);

        System.out.println("BENCHC|entorno|anioId=" + anioId + "|asignacionesUsadas=" + elegidas.size()
                + "|aulas=" + aulas.size() + "|bloquesExistentes=" + existentes.values().stream().mapToInt(List::size).sum());

        List<ConfirmarCargaMasivaRequest.BloqueConfirmacion> todos = generar(anioId, elegidas, aulas, existentes);
        System.out.println("BENCHC|capacidad sin conflictos|bloques=" + todos.size());

        ejecutar("calentamiento", anioId, todos.subList(0, Math.min(100, todos.size())));
        for (int n : new int[]{500, 1000, todos.size()}) {
            ejecutar("confirmar", anioId, todos.subList(0, Math.min(n, todos.size())));
        }
    }

    private List<Asig> mayorConjuntoSinChoques(List<Asig> asignaciones) {
        List<Asig> mejor = List.of();
        for (int inicio = 0; inicio < asignaciones.size(); inicio++) {
            List<Asig> actual = new ArrayList<>();
            Set<Long> secciones = new HashSet<>();
            Set<Long> docentes = new HashSet<>();
            for (int k = 0; k < asignaciones.size(); k++) {
                Asig a = asignaciones.get((inicio + k) % asignaciones.size());
                // Se acepta solo si no comparte sección ni docente con las ya elegidas.
                if (!secciones.contains(a.seccionId()) && !docentes.contains(a.docenteId())) {
                    actual.add(a);
                    secciones.add(a.seccionId());
                    docentes.add(a.docenteId());
                }
            }
            if (actual.size() > mejor.size()) {
                mejor = actual;
            }
        }
        return mejor;
    }

    /** Bloques de un minuto, en la jornada y fuera del receso, que no tocan ningún bloque ya guardado. */
    private List<ConfirmarCargaMasivaRequest.BloqueConfirmacion> generar(
            Long anioId, List<Asig> elegidas, List<Long> aulas, Map<String, List<Intervalo>> existentes) {
        List<ConfirmarCargaMasivaRequest.BloqueConfirmacion> bloques = new ArrayList<>();
        for (int minuto = 8 * 60; minuto < 13 * 60 + 30; minuto++) {
            if (minuto + 1 > 11 * 60 && minuto < 11 * 60 + 30) {
                continue;
            }
            LocalTime inicio = LocalTime.of(minuto / 60, minuto % 60);
            LocalTime fin = inicio.plusMinutes(1);
            for (DiaSemana dia : DiaSemana.values()) {
                boolean ocupado = existentes.getOrDefault(dia.name(), List.of()).stream()
                        .anyMatch(e -> e.inicio().isBefore(fin) && inicio.isBefore(e.fin()));
                if (ocupado) {
                    continue;
                }
                for (int k = 0; k < elegidas.size(); k++) {
                    bloques.add(new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                            elegidas.get(k).id(), aulas.get(k % aulas.size()), dia, inicio, fin,
                            "bench#" + bloques.size()));
                }
            }
        }
        return bloques;
    }

    private void ejecutar(String etiqueta, Long anioId,
                          List<ConfirmarCargaMasivaRequest.BloqueConfirmacion> bloques) {
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();
        TransactionTemplate tx = new TransactionTemplate(txManager);
        Marcas marcas = new Marcas();
        long[] tiempos = new long[3];
        ConfirmarCargaMasivaResponse[] respuesta = new ConfirmarCargaMasivaResponse[1];

        // Red de seguridad: si algún insert no participara en la transacción, quedaría guardado de verdad.
        Long maxIdAntes = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM bloques_horario", Long.class);
        Integer filasAntes = jdbc.queryForObject("SELECT COUNT(*) FROM bloques_horario", Integer.class);
        long[] verificadas = new long[1];

        st.clear();
        tiempos[0] = System.nanoTime();
        tx.execute(status -> {
            respuesta[0] = servicio.confirmar(new ConfirmarCargaMasivaRequest(anioId, bloques), null, marcas);
            tiempos[1] = System.nanoTime();
            // Dentro de la transacción las filas deben verse y coincidir con lo enviado, en el mismo orden.
            List<Map<String, Object>> guardadas = jdbc.queryForList(
                    "SELECT asignacion_curso_id, aula_id, dia_semana, hora_inicio, hora_fin "
                            + "FROM bloques_horario WHERE id > ? ORDER BY id", maxIdAntes);
            verificadas[0] = guardadas.size();
            for (int i = 0; i < guardadas.size() && i < bloques.size(); i++) {
                Map<String, Object> fila = guardadas.get(i);
                ConfirmarCargaMasivaRequest.BloqueConfirmacion esperado = bloques.get(i);
                boolean coincide = ((Number) fila.get("asignacion_curso_id")).longValue() == esperado.asignacionCursoId()
                        && ((Number) fila.get("aula_id")).longValue() == esperado.aulaId()
                        && fila.get("dia_semana").equals(esperado.diaSemana().name())
                        && fila.get("hora_inicio").toString().startsWith(esperado.horaInicio().toString())
                        && fila.get("hora_fin").toString().startsWith(esperado.horaFin().toString());
                if (!coincide) {
                    throw new IllegalStateException("La fila " + i + " guardada no coincide con lo enviado: " + fila);
                }
            }
            status.setRollbackOnly();
            return null;
        });
        tiempos[2] = System.nanoTime();

        Integer filasDespues = jdbc.queryForObject("SELECT COUNT(*) FROM bloques_horario", Integer.class);
        if (!filasAntes.equals(filasDespues)) {
            int borradas = jdbc.update("DELETE FROM bloques_horario WHERE id > ?", maxIdAntes);
            throw new IllegalStateException("Los inserts NO participaron en la transacción: quedaron " + borradas
                    + " filas, que se borraron. Se aborta la medición.");
        }
        if (verificadas[0] != bloques.size()) {
            throw new IllegalStateException("Se esperaban " + bloques.size() + " filas dentro de la transacción y hubo "
                    + verificadas[0]);
        }

        if (respuesta[0].bloquesCreados() != bloques.size()) {
            System.out.println("BENCHC|" + etiqueta + "|ERROR|creados=" + respuesta[0].bloquesCreados()
                    + "|errores=" + respuesta[0].errores().stream().limit(3).toList());
            return;
        }

        StringBuilder fases = new StringBuilder();
        for (int i = 1; i < marcas.nombres.size(); i++) {
            fases.append(String.format("%s=%.0fms|", marcas.nombres.get(i - 1),
                    (marcas.instantes.get(i) - marcas.instantes.get(i - 1)) / 1_000_000.0));
        }
        fases.append(String.format("%s=%.0fms",
                marcas.nombres.get(marcas.nombres.size() - 1),
                (tiempos[1] - marcas.instantes.get(marcas.instantes.size() - 1)) / 1_000_000.0));

        double total = (tiempos[1] - tiempos[0]) / 1_000_000.0;
        System.out.printf("BENCHC|%s|bloques=%d|confirmar_ms=%.0f|ms_por_bloque=%.3f|%s|sentencias_jdbc=%d|inserts_entidad=%d|rollback_ms=%.0f%n",
                etiqueta, bloques.size(), total, total / bloques.size(), fases, st.getPrepareStatementCount(),
                st.getEntityInsertCount(), (tiempos[2] - tiempos[1]) / 1_000_000.0);
    }
}
