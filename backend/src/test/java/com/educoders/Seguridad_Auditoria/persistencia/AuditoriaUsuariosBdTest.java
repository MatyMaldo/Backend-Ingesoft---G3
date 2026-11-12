package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ActualizarUsuarioRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.AuditoriaResponseDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DocenteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarDocenteRequest;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import com.educoders.Seguridad_Auditoria.servicio.AutenticacionService;
import com.educoders.Seguridad_Auditoria.servicio.DocenteService;
import com.educoders.Seguridad_Auditoria.servicio.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificación manual contra la base de datos real (no corre en {@code mvn test}) de la auditoría de usuarios y de
 * sesión (HU-SAT-04, HU-SAT-07) con los servicios reales. Todo corre en una transacción que SIEMPRE se revierte: los
 * triggers de la BD impiden borrar filas de {@code auditorias}, así que nada debe confirmarse. Se activa con
 * {@code -Dtest.bd=true}.
 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "test.bd", matches = "true")
class AuditoriaUsuariosBdTest {

    private static final String CLAVE = "ClaveDePrueba123";

    @Autowired DocenteService docenteService;
    @Autowired UsuarioService usuarioService;
    @Autowired AutenticacionService autenticacionService;
    @Autowired AuditoriaService auditoriaService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    private final String sufijo = String.valueOf(System.nanoTime());
    private final String correo = "zzz.audit." + sufijo + "@gmail.com";
    private final String dni = "9" + sufijo.substring(sufijo.length() - 7);

    @BeforeEach
    void autenticarComoAdministrador() {
        Long idAdmin = jdbc.queryForObject("SELECT MIN(id) FROM usuarios", Long.class);
        Administrador admin = new Administrador();
        admin.setId(idAdmin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private DocenteResponse registrarDocentePrueba() {
        return docenteService.registrarDocente(new RegistrarDocenteRequest(
                "ZzzAudit", "Prueba", "Uno", dni, correo, "900000001"));
    }

    private List<AuditoriaResponseDTO> auditoriasDe(String entidad, Long entidadId) {
        return auditoriaService.obtenerHistorialPorEntidad(entidad, entidadId, PageRequest.of(0, 50)).getContent();
    }

    @Test
    void registroEdicionBajaYAltaDejanUnRegistroCadaUno() {
        Long idAdmin = ((Administrador) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
        DocenteResponse docente = registrarDocentePrueba();

        usuarioService.actualizar(docente.id(), new ActualizarUsuarioRequest("ZzzNuevo", null, null, null, null, "955555555"));
        usuarioService.desactivar(docente.id());
        usuarioService.reactivar(docente.id());

        List<AuditoriaResponseDTO> historial = auditoriasDe("Usuario", docente.id());
        assertEquals(List.of("Registro de usuario", "Edición de usuario", "Desactivación de usuario", "Reactivación de usuario"),
                historial.stream().map(AuditoriaResponseDTO::getDescripcion).toList());
        assertTrue(historial.stream().allMatch(a -> idAdmin.equals(a.getUsuarioId())), "el autor es el administrador");
        assertEquals(List.of("CREAR", "ACTUALIZAR", "CAMBIO_ESTADO", "CAMBIO_ESTADO"),
                historial.stream().map(AuditoriaResponseDTO::getAccion).toList());

        var edicion = historial.get(1).getDetalles();
        assertEquals(2, edicion.size());
        assertEquals("Nombre", edicion.get(0).getCampo());
        assertEquals("ZzzAudit", edicion.get(0).getValorAnterior());
        assertEquals("ZzzNuevo", edicion.get(0).getValorNuevo());
        assertEquals("Teléfono", edicion.get(1).getCampo());
        assertEquals("900000001", edicion.get(1).getValorAnterior());
        assertEquals("955555555", edicion.get(1).getValorNuevo());

        String enBd = jdbc.queryForObject(
                "SELECT GROUP_CONCAT(d.campo, '=', IFNULL(d.valor_nuevo, '') SEPARATOR ';') FROM detalles_auditoria d "
                        + "JOIN auditorias a ON a.id = d.auditoria_id WHERE a.entidad = 'Usuario' AND a.entidad_id = ? "
                        + "AND a.descripcion = 'Registro de usuario'", String.class, docente.id());
        assertNotNull(enBd);
        assertTrue(enBd.contains("Rol=Docente") && enBd.contains("Estado=Activo"), enBd);
    }

    @Test
    void laBusquedaPorNombreEncuentraLosRegistrosDeUnUsuarioRecienCreado() {
        registrarDocentePrueba();

        var pagina = auditoriaService.listarAuditorias(null, TipoAccionAuditoria.CREAR, null, null,
                "registro de usuario", PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fechaHora")));

        assertTrue(pagina.getTotalElements() >= 1);
    }

    @Test
    void inicioFallidoYCierreDeSesionQuedanEnLaAuditoria() {
        DocenteResponse docente = registrarDocentePrueba();
        usuarioRepository.findById(docente.id()).ifPresent(u -> u.setPasswordHash(passwordEncoder.encode(CLAVE)));
        usuarioRepository.flush();

        // Contraseña incorrecta: el autor es el titular de la cuenta.
        assertThrows(ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(new LoginRequest(correo, "incorrecta")));
        // Correo que no pertenece a ninguna cuenta: autor "No determinado".
        assertThrows(ResponseStatusException.class,
                () -> autenticacionService.iniciarSesion(new LoginRequest("zzz.nadie." + sufijo + "@gmail.com", CLAVE)));
        // Inicio de sesión correcto y cierre voluntario.
        assertEquals("DOCENTE", autenticacionService.iniciarSesion(new LoginRequest(correo, CLAVE)).usuario().rol());
        autenticacionService.cerrarSesion(docente.id());

        List<AuditoriaResponseDTO> sesion = auditoriasDe("Sesion", docente.id());
        assertEquals(List.of("Intento fallido de inicio de sesión", "Inicio de sesión", "Cierre de sesión"),
                sesion.stream().map(AuditoriaResponseDTO::getDescripcion).toList());
        assertTrue(sesion.stream().allMatch(a -> docente.id().equals(a.getUsuarioId())));
        assertTrue(sesion.stream().allMatch(a -> "LOGIN".equals(a.getAccion())));
        assertEquals("Sin sesión", sesion.get(1).getDetalles().get(0).getValorAnterior());
        assertEquals("Sesión iniciada", sesion.get(1).getDetalles().get(0).getValorNuevo());
        assertEquals("Sesión iniciada", sesion.get(2).getDetalles().get(0).getValorAnterior());
        assertEquals("Sin sesión", sesion.get(2).getDetalles().get(0).getValorNuevo());

        // El del correo inexistente no tiene usuario ni entidadId: se ve como "No determinado".
        var desconocido = auditoriaService.listarAuditorias(null, TipoAccionAuditoria.LOGIN, null, null,
                "intento fallido", PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fechaHora"))).getContent().stream()
                .filter(a -> a.getUsuarioId() == null).findFirst().orElseThrow();
        assertEquals("No determinado", desconocido.getUsuario().nombre());
    }

    @Test
    void noGuardaContrasenasNiHashesEnLosDetalles() {
        DocenteResponse docente = registrarDocentePrueba();
        usuarioRepository.findById(docente.id()).ifPresent(u -> u.setPasswordHash(passwordEncoder.encode(CLAVE)));
        usuarioRepository.flush();
        autenticacionService.iniciarSesion(new LoginRequest(correo, CLAVE));

        Integer filtradas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detalles_auditoria WHERE valor_nuevo LIKE '$2a$%' OR valor_anterior LIKE '$2a$%' "
                        + "OR valor_nuevo LIKE ? OR valor_anterior LIKE ?", Integer.class, "%" + CLAVE + "%", "%" + CLAVE + "%");
        assertEquals(0, filtradas);
    }
}
