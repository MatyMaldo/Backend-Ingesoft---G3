package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.operaciones.ProgresoLote;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaUsuariosRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaUsuariosRequest.Fila;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaUsuariosRequest.TipoUsuario;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ResultadoFilaUsuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificación manual (no corre en {@code mvn test}) de la carga masiva con POCOS registros contra la BD local, con el
 * esquema nuevo (apoderados.fecha_nacimiento). Todo corre dentro de UNA transacción externa que se revierte, y la
 * auditoría (que se confirma aparte y no se puede borrar) se simula: no queda ningún dato en la base. Se activa con
 * {@code -Dtest.bd=true}.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "test.bd", matches = "true")
class CargaMasivaPocosRegistrosBdTest {

    @MockBean AuditoriaService auditoriaService;
    @Autowired CargaMasivaUsuariosService cargaMasiva;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void tresUsuariosValidosYTresRechazadosConElEsquemaNuevo() {
        Integer antes = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class);

        List<Fila> filas = List.of(
                new Fila(2, TipoUsuario.APODERADO, "Rosa", "Quispe", "Mamani", "81000001", "rosa@pocos-registros.test",
                        "981001001", LocalDate.of(1985, 3, 12), null, null),
                new Fila(3, TipoUsuario.DOCENTE, "Luis", "Huamán", null, "81000002", "prueba.luis@pocos-registros.test",
                        null, null, null, null),
                new Fila(4, TipoUsuario.ESTUDIANTE, "Ana", "Pérez", "Ruiz", "81000003", "prueba.ana@pocos-registros.test",
                        "981001003", LocalDate.of(2012, 5, 10), "81000001", "PRUEBA-POCOS-001"),
                // Rechazadas: apoderado sin fecha, apoderado con fecha futura y apoderado con DNI de 9 dígitos.
                new Fila(5, TipoUsuario.APODERADO, "Sin", "Fecha", null, "81000004", "sinfecha@pocos-registros.test",
                        null, null, null, null),
                new Fila(6, TipoUsuario.APODERADO, "Fecha", "Futura", null, "81000005", "futura@pocos-registros.test",
                        null, LocalDate.now().plusDays(3), null, null),
                new Fila(7, TipoUsuario.APODERADO, "Carnet", "Extranjeria", null, "810000066", "carnet@pocos-registros.test",
                        null, LocalDate.of(1980, 1, 1), null, null));

        @SuppressWarnings("unchecked")
        List<ResultadoFilaUsuario>[] resultado = new List[1];

        new TransactionTemplate(txManager).execute(status -> {
            cargaMasiva.cargar(new CargaMasivaUsuariosRequest(filas), null, new ProgresoLote() {
                public void iniciar(int total) { }
                public void actualizar(int p, int e, int o, int er) { }
                public void registrarError(String m) { }
                public void fase(String m) { }
                @SuppressWarnings("unchecked")
                public void resultado(Object r) {
                    resultado[0] = (List<ResultadoFilaUsuario>) r;
                }
            });

            resultado[0].forEach(r -> System.out.println("POCOS|fila=" + r.linea() + "|" + r.estado()
                    + (r.motivo() == null ? "" : "|motivo=" + r.motivo()) + (r.codigo() == null ? "" : "|codigo=" + r.codigo())));

            Apoderado rosa = (Apoderado) org.hibernate.Hibernate.unproxy(
                    usuarioRepository.findByEmail("rosa@pocos-registros.test").orElseThrow());
            System.out.println("POCOS|apoderado|nacimiento=" + rosa.getFechaNacimiento() + "|edad=" + rosa.getEdad()
                    + "|password=" + rosa.getPasswordHash());
            assertEquals(LocalDate.of(1985, 3, 12), rosa.getFechaNacimiento());
            assertNotNull(rosa.getEdad());
            assertNull(rosa.getPasswordHash(), "la cuenta nace sin contraseña");

            Integer enBd = jdbc.queryForObject("SELECT COUNT(*) FROM apoderados WHERE fecha_nacimiento = '1985-03-12'"
                    + " AND id = ?", Integer.class, rosa.getId());
            assertEquals(1, enBd, "la fecha quedó guardada en apoderados.fecha_nacimiento");
            status.setRollbackOnly();
            return null;
        });

        long creadas = resultado[0].stream().filter(r -> "CREADO".equals(r.estado())).count();
        long rechazadas = resultado[0].stream().filter(r -> "RECHAZADO".equals(r.estado())).count();
        assertEquals(3, creadas);
        assertEquals(3, rechazadas);
        assertTrue(resultado[0].stream().filter(r -> r.linea() >= 5).allMatch(r -> "RECHAZADO".equals(r.estado())));

        Integer despues = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class);
        assertEquals(antes, despues, "no debe quedar ningún usuario guardado");
    }
}
