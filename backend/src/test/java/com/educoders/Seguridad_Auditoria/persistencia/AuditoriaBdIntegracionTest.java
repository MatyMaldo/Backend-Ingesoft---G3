package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.dominio.Auditoria;
import com.educoders.Seguridad_Auditoria.presentacion.dto.AuditoriaResponseDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificación manual contra la base de datos real (no corre en {@code mvn test}): valida que la consulta de
 * búsqueda de auditorías es aceptada por Hibernate/MySQL (HU-SAT-04, Esc. 4) y que los registros son inmutables
 * (Esc. 1). Cada prueba corre en una transacción que SIEMPRE se revierte, así que no queda ningún registro.
 * Se activa con {@code -Dtest.bd=true}.
 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "test.bd", matches = "true")
class AuditoriaBdIntegracionTest {

    private static final String MARCA = "Zzzprueba100%bd";

    @Autowired AuditoriaRepository repositorio;
    @Autowired AuditoriaService servicio;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    private Auditoria guardar(Long usuarioId, String descripcion, CategoriaAuditoria categoria) {
        Auditoria a = new Auditoria();
        a.setFechaHora(LocalDateTime.now());
        a.setUsuarioId(usuarioId);
        a.setCategoria(categoria);
        a.setAccion(TipoAccionAuditoria.ACTUALIZAR);
        a.setEntidad("PruebaBd");
        a.setEntidadId(1L);
        a.setDescripcion(descripcion);
        return repositorio.saveAndFlush(a);
    }

    private List<Long> ids(String busqueda, CategoriaAuditoria categoria) {
        var pagina = servicio.listarAuditorias(categoria, null, null, null, busqueda,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "fechaHora")));
        return pagina.getContent().stream().map(AuditoriaResponseDTO::getId).toList();
    }

    @Test
    void labusquedaFiltraPorDescripcionUsuarioYSistemaEnLaBdReal() {
        Map<String, Object> usuario = jdbc.queryForMap(
                "SELECT id, nombre, apellido_paterno FROM usuarios ORDER BY id LIMIT 1");
        Long usuarioId = ((Number) usuario.get("id")).longValue();
        String nombre = (String) usuario.get("nombre");

        Auditoria deUsuario = guardar(usuarioId, MARCA + " usuario", CategoriaAuditoria.SISTEMA);
        Auditoria deSistema = guardar(null, MARCA + " sistema", CategoriaAuditoria.SEGURIDAD);

        long totalEnBd = jdbc.queryForObject("SELECT COUNT(*) FROM auditorias", Long.class);
        System.out.println("[BD] auditorías en la BD (incluye 2 de prueba): " + totalEnBd);

        // Sin texto: no filtra y el total (con count query) coincide con la tabla.
        var sinFiltro = servicio.listarAuditorias(null, null, null, null, null, PageRequest.of(0, 10));
        assertEquals(totalEnBd, sinFiltro.getTotalElements());

        // Por descripción (sin distinguir mayúsculas): el '%' del texto se busca literalmente.
        assertEquals(List.of(deSistema.getId(), deUsuario.getId()).stream().sorted().toList(),
                ids("zzzprueba100%bd", null).stream().sorted().toList());
        // '_' y '%' escapados: no actúan como comodines.
        assertTrue(ids("zzzprueba100_bd", null).isEmpty());
        assertTrue(ids("zzzprueba1%", null).isEmpty());

        // Por nombre del usuario que ejecutó la acción.
        assertTrue(ids(nombre, null).contains(deUsuario.getId()), "debe encontrar por nombre: " + nombre);

        // Los procesos automáticos (sin usuario) se encuentran con "sistema".
        assertTrue(ids("sistema", null).contains(deSistema.getId()));

        // Se combina con los demás filtros.
        assertEquals(List.of(deSistema.getId()), ids(MARCA, CategoriaAuditoria.SEGURIDAD));
        assertTrue(ids("zzz-no-existe-" + System.nanoTime(), null).isEmpty());
    }

    @Test
    void unRegistroGuardadoNoSeModifica() {
        Auditoria a = guardar(null, "original", CategoriaAuditoria.SISTEMA);
        Long id = a.getId();
        assertNotNull(id);

        a.setDescripcion("alterada");
        em.flush();
        em.clear();

        String enBd = jdbc.queryForObject("SELECT descripcion FROM auditorias WHERE id = ?", String.class, id);
        assertEquals("original", enBd);
    }

    @Test
    void unRegistroGuardadoNoSeElimina() {
        Auditoria a = guardar(null, "permanente", CategoriaAuditoria.SISTEMA);
        Long id = a.getId();

        assertThrows(RuntimeException.class, () -> {
            em.remove(em.find(Auditoria.class, id));
            em.flush();
        });
    }
}
