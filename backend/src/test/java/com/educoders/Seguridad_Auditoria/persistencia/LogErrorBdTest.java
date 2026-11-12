package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Infraestructura_Servicios.enums.Modulo;
import com.educoders.Infraestructura_Servicios.enums.TipoError;
import com.educoders.Seguridad_Auditoria.dominio.LogError;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LogErrorResponse;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificación manual contra la base de datos real (no corre en {@code mvn test}) del log de errores (HU-SAT-05 y
 * HU-SAT-06): consulta con el tipo de error como enum, lectura de registros antiguos, usuario involucrado e
 * inmutabilidad. Todo corre en una transacción que SIEMPRE se revierte. Se activa con {@code -Dtest.bd=true}.
 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "test.bd", matches = "true")
class LogErrorBdTest {

    private final String marca = "Zzzlog" + System.nanoTime();

    @Autowired LogErrorRepository repositorio;
    @Autowired LogErrorService servicio;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    private LogError guardar(TipoError tipo, Modulo modulo, String mensaje) {
        return repositorio.save(new LogError(LocalDateTime.now(), null, modulo, tipo, mensaje, null,
                "/api/v1/pagos", "POST", (short) 500));
    }

    private Page<LogErrorResponse> listar(Modulo modulo, String busqueda) {
        return servicio.listarErrores(null, null, modulo == null ? null : modulo.name(), busqueda,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "fechaHora")));
    }

    @Test
    void guardaLaEtiquetaDelTipoYLaConsultaConFiltrosYBusqueda() {
        LogError automatico = guardar(TipoError.PROCESO_AUTOMATICO, Modulo.MATRICULA, marca + " cierre");
        LogError correo = guardar(TipoError.NOTIFICACION_CORREO, Modulo.PAGOS, marca + " correo");
        em.flush();

        String enBd = jdbc.queryForObject("SELECT tipo_error FROM logs_error WHERE id = ?", String.class, automatico.getId());
        assertEquals("Proceso automático", enBd);

        // Búsqueda de texto + módulo: la consulta con el enum convertido es válida en Hibernate/MySQL.
        Page<LogErrorResponse> soloMatricula = listar(Modulo.MATRICULA, marca);
        assertEquals(1, soloMatricula.getTotalElements());
        LogErrorResponse respuesta = soloMatricula.getContent().get(0);
        assertEquals("Proceso automático", respuesta.tipoError());
        assertEquals("Sistema", respuesta.nombreUsuario(), "un proceso automático sin usuario es 'Sistema'");

        Page<LogErrorResponse> soloPagos = listar(Modulo.PAGOS, marca);
        assertEquals(1, soloPagos.getTotalElements());
        assertEquals("Notificación por correo", soloPagos.getContent().get(0).tipoError());
        assertEquals("No determinado", soloPagos.getContent().get(0).nombreUsuario(),
                "sin usuario y sin ser proceso automático el usuario es 'No determinado'");

        assertEquals(2, listar(null, marca).getTotalElements());
        assertEquals(0, listar(null, marca + "-no-existe").getTotalElements());
        assertEquals(correo.getId(), soloPagos.getContent().get(0).id());
    }

    @Test
    void losRegistrosAntiguosConTiposFueraDeLaListaSeLeenSinFallar() {
        // La base real puede conservar 'Recurso no encontrado' o 'ErrorInterno' de antes de la lista cerrada.
        jdbc.update("INSERT INTO logs_error (fecha_hora, modulo, tipo_error, mensaje) VALUES (NOW(), 'OTROS', 'ErrorInterno', ?)",
                marca + " antiguo");

        Page<LogErrorResponse> pagina = listar(null, marca);

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Excepción no controlada", pagina.getContent().get(0).tipoError());
    }

    @Test
    void elListadoGeneralDeLaBaseRealSeLeeCompleto() {
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM logs_error", Long.class);

        Page<LogErrorResponse> pagina = servicio.listarErrores(null, null, null, null, PageRequest.of(0, 10));

        assertEquals(total, pagina.getTotalElements());
        assertTrue(pagina.getContent().stream().allMatch(e -> e.tipoError() != null && e.nombreUsuario() != null));
    }

    @Test
    void unRegistroGuardadoNoSeModificaNiSeElimina() {
        LogError log = guardar(TipoError.EXCEPCION_NO_CONTROLADA, Modulo.OTROS, marca + " permanente");
        Long id = log.getId();
        em.flush();

        log.setMensaje("alterado");
        em.flush();
        em.clear();
        assertEquals(marca + " permanente",
                jdbc.queryForObject("SELECT mensaje FROM logs_error WHERE id = ?", String.class, id));

        assertThrows(RuntimeException.class, () -> {
            em.remove(em.find(LogError.class, id));
            em.flush();
        });
    }
}
