package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GenerarSesionesRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GenerarSesionesResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewSesionesRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewSesionesResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.SesionOmitida;
import com.educoders.Gestion_Matriz_Horarios.servicio.SesionClaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificación manual contra la base de datos real (no corre en {@code mvn test}) de la vista previa y la generación
 * de sesiones con la consulta única de existentes y la inserción en lote. Todo corre dentro de una transacción que
 * SIEMPRE se revierte, así que no deja sesiones. Usa la primera sección del año académico con bloques de horario
 * y un periodo de matrícula con la generación habilitada. Se activa con {@code -Dtest.bd=true}.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "test.bd", matches = "true")
class GeneracionSesionesBdTest {

    @Autowired SesionClaseService servicio;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    private record Seccion(long id, long anioAcademicoId, LocalDate inicio, LocalDate fin) {}

    /** Sección con bloques de un año ya iniciado, y el rango permitido de ese año. */
    private Seccion seccionConBloques() {
        return jdbc.query("""
                SELECT s.id, s.anio_academico_id, b.fecha_inicio, a.fecha_fin
                FROM secciones s
                JOIN anios_academicos a ON a.id = s.anio_academico_id
                JOIN bimestres b ON b.anio_academico_id = a.id AND b.nombre = 'BIM_1'
                WHERE b.fecha_inicio <= CURRENT_DATE
                  AND EXISTS (SELECT 1 FROM asignaciones_curso ac JOIN bloques_horario bh ON bh.asignacion_curso_id = ac.id
                              WHERE ac.seccion_id = s.id)
                ORDER BY s.id LIMIT 1
                """, rs -> rs.next()
                ? new Seccion(rs.getLong(1), rs.getLong(2), rs.getDate(3).toLocalDate(), rs.getDate(4).toLocalDate())
                : null);
    }

    private long sesionesDe(long seccionId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM sesiones_clase sc
                JOIN bloques_horario bh ON bh.id = sc.bloque_horario_id
                JOIN asignaciones_curso ac ON ac.id = bh.asignacion_curso_id
                WHERE ac.seccion_id = ?
                """, Long.class, seccionId);
    }

    /** La columna `estado` de la BD local puede ser anterior a la omisión de sesiones y no incluir OMITIDA. */
    private boolean laBaseAdmiteOmitidas() {
        String tipo = jdbc.queryForObject("""
                SELECT COLUMN_TYPE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sesiones_clase' AND COLUMN_NAME = 'estado'
                """, String.class);
        return tipo != null && tipo.contains("'OMITIDA'");
    }

    private long omitidasDe(long seccionId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM sesiones_clase sc
                JOIN bloques_horario bh ON bh.id = sc.bloque_horario_id
                JOIN asignaciones_curso ac ON ac.id = bh.asignacion_curso_id
                WHERE ac.seccion_id = ? AND sc.estado = 'OMITIDA'
                """, Long.class, seccionId);
    }

    @Test
    void laVistaPreviaYLaGeneracionCoincidenYSeRevierten() {
        Seccion seccion = seccionConBloques();
        assertTrue(seccion != null, "la BD local necesita una sección con bloques en un año ya iniciado");

        PreviewSesionesRequest preview = new PreviewSesionesRequest();
        preview.setAnioAcademicoId(seccion.anioAcademicoId());
        preview.setSeccionId(seccion.id());
        preview.setFechaInicio(seccion.inicio());
        preview.setFechaFin(seccion.fin());

        long antes = sesionesDe(seccion.id());
        long[] medidas = new long[6];
        new TransactionTemplate(txManager).executeWithoutResult(estado -> {
            long t0 = System.nanoTime();
            PreviewSesionesResponse vista = servicio.previewSesiones(preview);
            medidas[0] = (System.nanoTime() - t0) / 1_000_000;

            long yaExisten = vista.getSesiones().stream().filter(s -> s.isYaExiste()).count();
            long nuevas = vista.getTotal() - yaExisten;
            // Una de las nuevas se pide como omitida.
            var primeraNueva = vista.getSesiones().stream().filter(s -> !s.isYaExiste()).findFirst().orElseThrow();

            GenerarSesionesRequest generar = new GenerarSesionesRequest();
            generar.setAnioAcademicoId(seccion.anioAcademicoId());
            generar.setSeccionId(seccion.id());
            generar.setFechaInicio(seccion.inicio());
            generar.setFechaFin(seccion.fin());
            boolean conOmitida = laBaseAdmiteOmitidas();
            if (conOmitida) {
                generar.setSesionesOmitidas(List.of(new SesionOmitida(primeraNueva.getBloqueHorarioId(), primeraNueva.getFecha())));
            } else {
                System.out.println("AVISO la columna sesiones_clase.estado de esta BD no incluye OMITIDA: se prueba sin omisiones");
            }

            long omitidasAntes = omitidasDe(seccion.id());
            long t1 = System.nanoTime();
            GenerarSesionesResponse resumen = servicio.generarSesiones(generar);
            medidas[1] = (System.nanoTime() - t1) / 1_000_000;

            // Lo que la vista previa dijo "por crear" es lo que se inserta, con la omitida marcada.
            assertEquals(nuevas, resumen.getSesionesCreadas() + resumen.getSesionesOmitidasCreadas());
            assertEquals(yaExisten, resumen.getSesionesYaExistentes());
            assertEquals(antes + nuevas, sesionesDe(seccion.id()), "el lote debe insertar exactamente las sesiones nuevas");
            assertEquals(omitidasAntes + (conOmitida ? 1 : 0), omitidasDe(seccion.id()),
                    "la sesión pedida como omitida se crea con estado OMITIDA");

            // Una segunda generación no duplica nada.
            GenerarSesionesResponse repetida = servicio.generarSesiones(generar);
            assertEquals(0, repetida.getSesionesCreadas() + repetida.getSesionesOmitidasCreadas());
            assertEquals(antes + nuevas, sesionesDe(seccion.id()));

            // La vista previa ahora ve todo como existente.
            PreviewSesionesResponse despues = servicio.previewSesiones(preview);
            assertTrue(despues.getSesiones().stream().allMatch(s -> s.isYaExiste()));
            medidas[2] = vista.getTotal();
            medidas[3] = nuevas;
            // No se confirma: la base no se toca.
            estado.setRollbackOnly();
        });

        assertEquals(antes, sesionesDe(seccion.id()), "al revertir no debe quedar ninguna sesión generada");
        System.out.println("MEDIDA preview=" + medidas[0] + " ms, generar=" + medidas[1]
                + " ms, sesiones previstas=" + medidas[2] + ", nuevas=" + medidas[3]);
    }
}
