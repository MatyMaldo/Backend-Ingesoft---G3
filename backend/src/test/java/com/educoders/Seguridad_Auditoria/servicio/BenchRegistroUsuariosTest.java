package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaUsuariosRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarApoderadoRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarDocenteRequest;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Medición manual (no corre en {@code mvn test}) del alta de usuarios que usa la carga masiva: la pantalla llama una vez
 * por fila a {@code POST /usuarios/docentes|apoderados|estudiantes}. Cada llamada corre en su propia transacción
 * que SIEMPRE se revierte (no queda ningún usuario; el commit real no entra en la medición y el AUTO_INCREMENT avanza).
 * Se activa con {@code -Dbench.usuarios=true}.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@EnabledIfSystemProperty(named = "bench.usuarios", matches = "true")
class BenchRegistroUsuariosTest {

    @Autowired CargaMasivaUsuariosService cargaMasiva;
    // La auditoría se confirma en una transacción aparte: se simula para que la medición no deje registros.
    @org.springframework.boot.test.mock.mockito.MockBean AuditoriaService auditoriaService;
    @Autowired DocenteService docenteService;
    @Autowired ApoderadoService apoderadoService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired EntityManagerFactory emf;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void medirAltas() {
        Long maxIdAntes = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM usuarios", Long.class);
        Integer filasAntes = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class);
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();

        // Equivalente al filtro JWT de cada petición: findById del usuario autenticado.
        Long idAdmin = jdbc.queryForObject("SELECT MIN(id) FROM usuarios", Long.class);
        medir("filtro JWT: findById (1.ª vez)", 1, st, i -> usuarioRepository.findById(idAdmin));
        medir("filtro JWT: findById (siguientes)", 30, st, i -> usuarioRepository.findById(idAdmin));

        medir("docente (1.ª vez, en frío)", 1, st, i -> docenteService.registrarDocente(
                new RegistrarDocenteRequest("Bench" + i, "Prueba", "Uno", "8" + String.format("%07d", i),
                        "bench" + i + "@bench-registro.test", "900000000")));
        medir("docentes (30 siguientes)", 30, st, i -> docenteService.registrarDocente(
                new RegistrarDocenteRequest("Bench" + i, "Prueba", "Uno", "7" + String.format("%07d", i),
                        "bench-b" + i + "@bench-registro.test", "900000000")));
        medir("apoderado (1.ª vez, en frío)", 1, st, i -> apoderadoService.registrarApoderado(
                new RegistrarApoderadoRequest("Bench" + i, "Prueba", "Uno", "6" + String.format("%07d", i),
                        "benchap" + i + "@bench-registro.test", "900000000", 40)));
        medir("apoderados (30 siguientes)", 30, st, i -> apoderadoService.registrarApoderado(
                new RegistrarApoderadoRequest("Bench" + i, "Prueba", "Uno", "5" + String.format("%07d", i),
                        "benchap-b" + i + "@bench-registro.test", "900000000", 40)));

        Integer filasDespues = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class);
        if (!filasAntes.equals(filasDespues)) {
            int borradas = jdbc.update("DELETE FROM usuarios WHERE id > ? AND email LIKE '%@bench-registro.test'", maxIdAntes);
            throw new IllegalStateException("Quedaron usuarios guardados; se borraron " + borradas);
        }
    }

    /**
     * Carga en lote (el camino nuevo) contra la BD local, dentro de UNA transacción externa que se revierte: los lotes
     * internos se unen a ella, así que no queda nada guardado. Verifica además que las filas se vean dentro de la
     * transacción (apoderado, docentes y estudiantes vinculados).
     */
    @Test
    void medirCargaEnLote() {
        Integer filasAntes = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class);
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();
        for (int[] caso : new int[][]{{10, 10, 10}, {10, 10, 10}, {100, 100, 300}}) {
            int apoderados = caso[0];
            int docentes = caso[1];
            int estudiantes = caso[2];
            List<CargaMasivaUsuariosRequest.Fila> filas = new ArrayList<>();
            int linea = 2;
            for (int i = 0; i < apoderados; i++) {
                filas.add(new CargaMasivaUsuariosRequest.Fila(linea++, CargaMasivaUsuariosRequest.TipoUsuario.APODERADO,
                        "Ap" + i, "Prueba", "Lote", "6" + String.format("%07d", i), "bench-lote-ap" + i + "@bench-lote.test",
                        "900000000", 40, null, null, null));
            }
            for (int i = 0; i < docentes; i++) {
                filas.add(new CargaMasivaUsuariosRequest.Fila(linea++, CargaMasivaUsuariosRequest.TipoUsuario.DOCENTE,
                        "Doc" + i, "Prueba", "Lote", "7" + String.format("%07d", i), "bench-lote-doc" + i + "@bench-lote.test",
                        null, null, null, null, null));
            }
            for (int i = 0; i < estudiantes; i++) {
                filas.add(new CargaMasivaUsuariosRequest.Fila(linea++, CargaMasivaUsuariosRequest.TipoUsuario.ESTUDIANTE,
                        "Est" + i, "Prueba", "Lote", "5" + String.format("%07d", i), "bench-lote-est" + i + "@bench-lote.test",
                        null, null, java.time.LocalDate.of(2012, 1, 1), "6" + String.format("%07d", i % apoderados),
                        "BLT" + i));
            }

            final List<com.educoders.Seguridad_Auditoria.presentacion.dto.ResultadoFilaUsuario>[] resultado = new List[1];
            st.clear();
            long t0 = System.nanoTime();
            new TransactionTemplate(txManager).execute(status -> {
                cargaMasiva.cargar(new CargaMasivaUsuariosRequest(filas), null, new com.educoders.Infraestructura_Servicios.operaciones.ProgresoLote() {
                    public void iniciar(int total) { }
                    public void actualizar(int p, int e, int o, int er) { }
                    public void registrarError(String m) { System.out.println("BENCHL|rechazo|" + m); }
                    public void fase(String m) { }
                    @SuppressWarnings("unchecked")
                    public void resultado(Object r) {
                        resultado[0] = (List<com.educoders.Seguridad_Auditoria.presentacion.dto.ResultadoFilaUsuario>) r;
                    }
                });
                Integer dentro = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM usuarios WHERE email LIKE '%@bench-lote.test'", Integer.class);
                Integer estudiantesVinculados = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM estudiantes e JOIN usuarios u ON u.id = e.id "
                                + "WHERE u.email LIKE '%@bench-lote.test' AND e.apoderado_id IS NOT NULL", Integer.class);
                // Lectura con JPA de lo insertado por JDBC: tipo, activo, sin contraseña, vínculo y código.
                for (String correo : new String[]{"bench-lote-ap0@bench-lote.test", "bench-lote-doc0@bench-lote.test",
                        "bench-lote-est0@bench-lote.test"}) {
                    var u = org.hibernate.Hibernate.unproxy(usuarioRepository.findByEmail(correo).orElse(null));
                    String extra = u instanceof com.educoders.Seguridad_Auditoria.dominio.Estudiante e
                            ? "|codigo=" + e.getCodigo() + "|nac=" + e.getFechaNacimiento() + "|apoderadoId=" + e.getApoderado().getId()
                            : u instanceof com.educoders.Seguridad_Auditoria.dominio.Apoderado a ? "|edad=" + a.getEdad() : "";
                    com.educoders.Seguridad_Auditoria.dominio.Usuario usr = (com.educoders.Seguridad_Auditoria.dominio.Usuario) u;
                    System.out.println("BENCHL|jpa|" + u.getClass().getSimpleName() + "|activo=" + usr.isActivo()
                            + "|password=" + usr.getPasswordHash() + "|username=" + usr.getUsername()
                            + "|creado=" + usr.getFechaCreacion() + extra);
                }
                System.out.println("BENCHL|dentro_de_la_transaccion|usuarios=" + dentro + "|estudiantesVinculados=" + estudiantesVinculados);
                status.setRollbackOnly();
                return null;
            });
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            long creadas = resultado[0].stream().filter(r -> "CREADO".equals(r.estado())).count();
            System.out.printf("BENCHL|filas=%d|creadas=%d|total_ms=%.0f|ms_por_fila=%.2f|sentencias=%d%n",
                    filas.size(), creadas, ms, ms / filas.size(), st.getPrepareStatementCount());
        }
        Integer filasDespues = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class);
        if (!filasAntes.equals(filasDespues)) {
            int borradas = jdbc.update("DELETE FROM usuarios WHERE email LIKE '%@bench-lote.test'");
            throw new IllegalStateException("Quedaron usuarios guardados; se borraron " + borradas);
        }
    }

    private void medir(String etiqueta, int veces, Statistics st, Consumer<Integer> accion) {
        List<Double> tiempos = new ArrayList<>();
        st.clear();
        for (int i = 0; i < veces; i++) {
            final int n = i;
            TransactionTemplate tx = new TransactionTemplate(txManager);
            long t0 = System.nanoTime();
            tx.execute(status -> {
                accion.accept(n);
                status.setRollbackOnly();
                return null;
            });
            tiempos.add((System.nanoTime() - t0) / 1_000_000.0);
        }
        double total = tiempos.stream().mapToDouble(Double::doubleValue).sum();
        double max = tiempos.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        System.out.printf("BENCHU|%s|llamadas=%d|total_ms=%.0f|promedio_ms=%.1f|max_ms=%.1f|sentencias_por_llamada=%.1f%n",
                etiqueta, veces, total, total / veces, max, st.getPrepareStatementCount() / (double) veces);
    }
}
