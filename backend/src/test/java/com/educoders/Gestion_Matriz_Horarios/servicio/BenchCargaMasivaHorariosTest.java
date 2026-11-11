package com.educoders.Gestion_Matriz_Horarios.servicio;

import jakarta.persistence.EntityManagerFactory;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewCargaMasivaResponse;

/**
 * Medición manual (no corre en {@code mvn test}) del preview de carga masiva de horarios.
 * <p>
 * Solo LEE de la base: el preview es {@code readOnly}. Se activa con
 * {@code -Dbench.horarios=true}. Imprime líneas con prefijo {@code BENCH|}.
 * Usa los datos que ya existan en la BD configurada (DB_URL).
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@EnabledIfSystemProperty(named = "bench.horarios", matches = "true")
class BenchCargaMasivaHorariosTest {

    private static final String[] DIAS = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"};

    @Autowired HorarioCargaMasivaService servicio;
    @Autowired EntityManagerFactory emf;
    @Autowired JdbcTemplate jdbc;

    private record Asig(long seccionId, String grado, String seccion, long cursoId, String curso,
                        long docenteId, String dni, String docente) {
    }

    private record AulaRef(long id, String nombre) {
    }

    /**
     * Valida un archivo real contra la BD configurada (solo lectura) y muestra el estado de cada fila.
     * Uso: {@code -Dbench.horarios=true -Dbench.archivo=C:/ruta/horarios.csv}.
     */
    @Test
    @EnabledIfSystemProperty(named = "bench.archivo", matches = ".+")
    void validarArchivo() throws Exception {
        Long anioId = jdbc.queryForObject(
                "SELECT anio_academico_id FROM periodos_matricula "
                        + "WHERE CURDATE() BETWEEN fecha_inicio AND fecha_fin LIMIT 1", Long.class);
        java.nio.file.Path ruta = java.nio.file.Path.of(System.getProperty("bench.archivo"));
        MockMultipartFile archivo = new MockMultipartFile("files", ruta.getFileName().toString(),
                "text/csv", java.nio.file.Files.readAllBytes(ruta));

        PreviewCargaMasivaResponse r = servicio.preview(List.of(archivo), anioId);
        var res = r.resumen();
        System.out.printf("BENCH|archivo|anioId=%d|filas=%d|validas=%d|errores=%d|duplicadas=%d|conflictos=%d%n",
                anioId, res.filas(), res.validas(), res.conError(), res.duplicadas(), res.conflictos());
        r.bloques().stream()
                .filter(b -> !"VALIDO".equals(b.estado()))
                .forEach(b -> System.out.println("BENCH|fila " + b.fila() + "|" + b.estado() + "|" + b.errores()));
    }

    @Test
    @org.junit.jupiter.api.condition.DisabledIfSystemProperty(named = "bench.archivo", matches = ".+")
    void medirPreview() throws Exception {
        Long anioId = jdbc.queryForObject(
                "SELECT anio_academico_id FROM periodos_matricula "
                        + "WHERE CURDATE() BETWEEN fecha_inicio AND fecha_fin LIMIT 1", Long.class);

        List<Asig> asignaciones = jdbc.query("""
                SELECT s.id, g.nombre, s.nombre, c.id, c.nombre, u.id, u.dni, u.nombre
                FROM asignaciones_curso ac
                JOIN secciones s ON s.id = ac.seccion_id
                JOIN grados g ON g.id = s.grado_id
                JOIN cursos c ON c.id = ac.curso_id
                JOIN usuarios u ON u.id = ac.docente_id
                WHERE s.anio_academico_id = ?
                """, (rs, i) -> new Asig(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getLong(4), rs.getString(5), rs.getLong(6), rs.getString(7), rs.getString(8)), anioId);
        List<AulaRef> aulas = jdbc.query("SELECT id, nombre FROM aulas",
                (rs, i) -> new AulaRef(rs.getLong(1), rs.getString(2)));
        Integer bloquesExistentes = jdbc.queryForObject("SELECT COUNT(*) FROM bloques_horario", Integer.class);

        System.out.println("BENCH|entorno|anioId=" + anioId + "|asignaciones=" + asignaciones.size()
                + "|aulas=" + aulas.size() + "|bloquesExistentes=" + bloquesExistentes);

        // Calentamiento (JIT, pool de conexiones, caché de metadatos); no se reporta como medición.
        ejecutar("calentamiento", "csv", filas(200, true, asignaciones, aulas, false), anioId);

        for (int n : new int[]{500, 1000, 2500, 5000}) {
            ejecutar("csv-ids", "csv", filas(n, true, asignaciones, aulas, false), anioId);
        }
        for (int n : new int[]{1000, 5000}) {
            ejecutar("csv-nombres", "csv", filas(n, false, asignaciones, aulas, false), anioId);
        }
        ejecutar("csv-solo-parseo(ids invalidos)", "csv", filas(5000, true, asignaciones, aulas, true), anioId);
        ejecutar("xlsx-ids", "xlsx", filas(5000, true, asignaciones, aulas, false), anioId);
        ejecutar("xlsx-solo-parseo(ids invalidos)", "xlsx", filas(5000, true, asignaciones, aulas, true), anioId);
        ejecutar("csv-ids(repeticion)", "csv", filas(5000, true, asignaciones, aulas, false), anioId);
    }

    /** Genera n filas con combinaciones únicas (asignación, aula, día, hora inicio, hora fin). */
    private List<String[]> filas(int n, boolean conIds, List<Asig> asignaciones, List<AulaRef> aulas,
                                 boolean idsInvalidos) {
        List<String[]> filas = new ArrayList<>();
        outer:
        for (int duracion : new int[]{15, 20, 25, 30, 35, 40, 45, 50, 55, 60}) {
            for (int ini = 8 * 60; ini + duracion <= 11 * 60; ini += 5) {
                for (String dia : DIAS) {
                    for (AulaRef aula : aulas) {
                        for (Asig a : asignaciones) {
                            filas.add(new String[]{
                                    idsInvalidos ? "x" : (conIds ? String.valueOf(a.seccionId()) : ""),
                                    a.grado(), a.seccion(),
                                    conIds ? String.valueOf(a.cursoId()) : "", a.curso(),
                                    conIds ? String.valueOf(a.docenteId()) : "", a.dni(), a.docente(),
                                    conIds ? String.valueOf(aula.id()) : "", aula.nombre(),
                                    dia, hora(ini), hora(ini + duracion)
                            });
                            if (filas.size() == n) {
                                break outer;
                            }
                        }
                    }
                }
            }
        }
        if (filas.size() != n) {
            throw new IllegalStateException("No se pudieron generar " + n + " filas únicas: " + filas.size());
        }
        return filas;
    }

    private static String hora(int minutos) {
        return String.format("%02d:%02d", minutos / 60, minutos % 60);
    }

    private MockMultipartFile archivo(String formato, List<String[]> filas) throws Exception {
        if ("xlsx".equals(formato)) {
            try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Sheet hoja = wb.createSheet("Horarios");
                Row cab = hoja.createRow(0);
                for (int c = 0; c < HorarioCargaMasivaServiceImpl.ENCABEZADOS.length; c++) {
                    cab.createCell(c).setCellValue(HorarioCargaMasivaServiceImpl.ENCABEZADOS[c]);
                }
                for (int r = 0; r < filas.size(); r++) {
                    Row fila = hoja.createRow(r + 1);
                    for (int c = 0; c < filas.get(r).length; c++) {
                        fila.createCell(c).setCellValue(filas.get(r)[c]);
                    }
                }
                wb.write(out);
                return new MockMultipartFile("files", "bench.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
            }
        }
        StringBuilder sb = new StringBuilder(String.join(";", HorarioCargaMasivaServiceImpl.ENCABEZADOS)).append("\r\n");
        for (String[] f : filas) {
            sb.append(String.join(";", f)).append("\r\n");
        }
        return new MockMultipartFile("files", "bench.csv", "text/csv", sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void ejecutar(String etiqueta, String formato, List<String[]> filas, Long anioId) throws Exception {
        MockMultipartFile archivo = archivo(formato, filas);
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();
        st.clear();
        long t0 = System.nanoTime();
        PreviewCargaMasivaResponse r = servicio.preview(List.of(archivo), anioId);
        double ms = (System.nanoTime() - t0) / 1_000_000.0;
        var res = r.resumen();
        System.out.printf("BENCH|%s|filas=%d|ms=%.0f|ms_por_fila=%.3f|sentencias_jdbc=%d|consultas=%d|entidades_cargadas=%d"
                        + "|validas=%d|errores=%d|duplicadas=%d|conflictos=%d%n",
                etiqueta, filas.size(), ms, ms / filas.size(), st.getPrepareStatementCount(),
                st.getQueryExecutionCount(), st.getEntityLoadCount(),
                res.validas(), res.conError(), res.duplicadas(), res.conflictos());
    }
}
