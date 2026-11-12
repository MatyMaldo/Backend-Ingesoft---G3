package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.dominio.Auditoria;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verificación manual contra la base de datos real (no corre en {@code mvn test}) de la atomicidad de la auditoría
 * (HU-SAT-04, Esc. 2): la operación y su registro se confirman juntos o ninguno se aplica.
 *
 * <p>IMPORTANTE: la base protege {@code auditorias} con triggers que prohíben UPDATE y DELETE, así que una prueba que
 * confirme filas no puede limpiarlas. Por eso aquí NINGUNA transacción se confirma: todas terminan revertidas y al
 * final se comprueba que no quedó nada. Se activa con {@code -Dtest.bd=true}.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "test.bd", matches = "true")
class AuditoriaAtomicidadBdTest {

    /** Marca única por prueba (JUnit crea una instancia por método), para no depender de filas ya existentes. */
    private final String marca = "Zzzatomica-" + System.nanoTime();

    @Autowired AuditoriaRepository repositorio;
    @Autowired AuditoriaService servicio;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    private long filasDePrueba() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM auditorias WHERE descripcion LIKE ?", Long.class, marca + "%");
    }

    /** Hace de "operación de negocio" auditada: una fila cualquiera guardada en la misma transacción. */
    private void operacionDeNegocio() {
        Auditoria operacion = new Auditoria();
        operacion.setFechaHora(LocalDateTime.now());
        operacion.setCategoria(CategoriaAuditoria.SISTEMA);
        operacion.setAccion(TipoAccionAuditoria.ACTUALIZAR);
        operacion.setEntidad("PruebaAtomicidad");
        operacion.setDescripcion(marca + "-operacion");
        repositorio.saveAndFlush(operacion);
    }

    private RegistrarAuditoriaRequestDTO dto(TipoAccionAuditoria accion) {
        return new RegistrarAuditoriaRequestDTO(
                null, CategoriaAuditoria.SISTEMA, accion, "PruebaAtomicidad", 1L, marca + "-registro", List.of());
    }

    @Test
    void sinTransaccionAbiertaLaAuditoriaSeRechaza() {
        assertThrows(IllegalTransactionStateException.class, () -> servicio.registrarAuditoria(dto(TipoAccionAuditoria.CREAR)));
        assertEquals(0, filasDePrueba());
    }

    @Test
    void siLaAuditoriaFallaLaOperacionNoSeAplica() {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(estado -> {
            operacionDeNegocio();
            // Acción nula: viola la restricción NOT NULL, así que la auditoría no puede guardarse.
            servicio.registrarAuditoria(dto(null));
        }));

        assertEquals(0, filasDePrueba(), "la operación debió revertirse junto con la auditoría fallida");
    }

    @Test
    void operacionYAuditoriaComparteLaMismaTransaccion() {
        long[] dentroDeLaTransaccion = new long[1];

        new TransactionTemplate(txManager).executeWithoutResult(estado -> {
            operacionDeNegocio();
            servicio.registrarAuditoria(dto(TipoAccionAuditoria.CREAR));
            dentroDeLaTransaccion[0] = filasDePrueba();
            // No se confirma: los triggers de la BD impiden borrar después. Revertir demuestra que ambas
            // filas dependían de la misma transacción: si la auditoría tuviera la suya, quedaría una fila.
            estado.setRollbackOnly();
        });

        assertEquals(2, dentroDeLaTransaccion[0], "ambas filas deben verse dentro de la misma transacción");
        assertEquals(0, filasDePrueba(), "al revertir la transacción no debe quedar ni la operación ni la auditoría");
    }
}
