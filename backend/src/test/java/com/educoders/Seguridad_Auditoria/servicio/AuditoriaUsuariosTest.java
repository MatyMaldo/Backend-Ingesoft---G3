package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleAuditoriaDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaUsuariosTest {

    private Apoderado apoderado() {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(3L);
        apoderado.setNombre("Rosa");
        apoderado.setApellidoPaterno("Quispe");
        apoderado.setApellidoMaterno("Rojas");
        apoderado.setDni("11111111");
        apoderado.setEmail("rosa@gmail.com");
        apoderado.setTelefono("900000000");
        apoderado.setFechaNacimiento(java.time.LocalDate.of(1985, 3, 12));
        apoderado.setActivo(true);
        return apoderado;
    }

    private String valorNuevo(RegistrarAuditoriaRequestDTO auditoria, String campo) {
        return auditoria.getDetalles().stream()
                .filter(d -> d.getCampo().equals(campo)).map(DetalleAuditoriaDTO::getValorNuevo).findFirst().orElse(null);
    }

    @Test
    void registroDeApoderadoDejaElValorAnteriorVacioYIncluyeSusDatos() {
        RegistrarAuditoriaRequestDTO auditoria = AuditoriaUsuarios.registro(apoderado());

        assertEquals(TipoAccionAuditoria.CREAR, auditoria.getAccion());
        assertEquals("Registro de usuario", auditoria.getDescripcion());
        assertEquals(3L, auditoria.getEntidadId());
        assertFalse(auditoria.isAutorFijo(), "el autor es el administrador autenticado");
        assertTrue(auditoria.getDetalles().stream().allMatch(d -> d.getValorAnterior() == null));
        assertEquals("Rosa", valorNuevo(auditoria, "Nombre"));
        assertEquals("Apoderado", valorNuevo(auditoria, "Rol"));
        assertEquals("Activo", valorNuevo(auditoria, "Estado"));
        assertEquals("1985-03-12", valorNuevo(auditoria, "Fecha de nacimiento"));
    }

    @Test
    void registroDeEstudianteIncluyeCodigoFechaYApoderado() {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(9L);
        estudiante.setNombre("Luis");
        estudiante.setApellidoPaterno("Quispe");
        estudiante.setDni("22222222");
        estudiante.setEmail("luis@gmail.com");
        estudiante.setCodigo("EST001");
        estudiante.setFechaNacimiento(LocalDate.of(2010, 5, 1));
        estudiante.setApoderado(apoderado());
        estudiante.setActivo(true);

        RegistrarAuditoriaRequestDTO auditoria = AuditoriaUsuarios.registro(estudiante);

        assertEquals("Estudiante", valorNuevo(auditoria, "Rol"));
        assertEquals("EST001", valorNuevo(auditoria, "Código"));
        assertEquals("2010-05-01", valorNuevo(auditoria, "Fecha de nacimiento"));
        assertEquals("Rosa Quispe Rojas", valorNuevo(auditoria, "Apoderado"));
    }

    @Test
    void edicionIncluyeSoloLosCamposQueCambiaronYIgnoraVaciosEquivalentes() {
        var antes = new AuditoriaUsuarios.DatosEditables("Rosa", "Quispe", null, "11111111", "rosa@gmail.com", "900000000");
        var despues = new AuditoriaUsuarios.DatosEditables("Rosa", "Quispe", "  ", "11111111", "nuevo@gmail.com", "900000000");

        RegistrarAuditoriaRequestDTO auditoria = AuditoriaUsuarios.edicion(apoderado(), antes, despues);

        assertEquals(1, auditoria.getDetalles().size());
        assertEquals("Correo electrónico", auditoria.getDetalles().get(0).getCampo());
        assertEquals("rosa@gmail.com", auditoria.getDetalles().get(0).getValorAnterior());
        assertEquals("nuevo@gmail.com", auditoria.getDetalles().get(0).getValorNuevo());
    }

    @Test
    void edicionSinCambiosNoGeneraRegistro() {
        var datos = AuditoriaUsuarios.datosEditables(apoderado());

        assertNull(AuditoriaUsuarios.edicion(apoderado(), datos, datos));
    }

    @Test
    void cambioDeEstadoRegistraElSentidoDelCambio() {
        RegistrarAuditoriaRequestDTO baja = AuditoriaUsuarios.cambioDeEstado(apoderado(), false);
        RegistrarAuditoriaRequestDTO alta = AuditoriaUsuarios.cambioDeEstado(apoderado(), true);

        assertEquals("Desactivación de usuario", baja.getDescripcion());
        assertEquals("Activo", baja.getDetalles().get(0).getValorAnterior());
        assertEquals("Inactivo", baja.getDetalles().get(0).getValorNuevo());
        assertEquals("Reactivación de usuario", alta.getDescripcion());
        assertEquals("Inactivo", alta.getDetalles().get(0).getValorAnterior());
        assertEquals("Activo", alta.getDetalles().get(0).getValorNuevo());
        assertEquals(TipoAccionAuditoria.CAMBIO_ESTADO, baja.getAccion());
    }

    @Test
    void ningunRegistroDeUsuarioGuardaContrasenasNiHashes() {
        Apoderado apoderado = apoderado();
        apoderado.setPasswordHash("$2a$10$hashSecreto");

        var registros = java.util.List.of(
                AuditoriaUsuarios.registro(apoderado),
                AuditoriaUsuarios.cambioDeEstado(apoderado, false),
                AuditoriaUsuarios.inicioDeSesion(apoderado),
                AuditoriaUsuarios.intentoFallido(apoderado),
                AuditoriaUsuarios.cierreDeSesion(3L));

        for (RegistrarAuditoriaRequestDTO registro : registros) {
            for (DetalleAuditoriaDTO detalle : registro.getDetalles()) {
                assertFalse(String.valueOf(detalle.getValorNuevo()).contains("hashSecreto"));
                assertFalse(String.valueOf(detalle.getValorAnterior()).contains("hashSecreto"));
                assertFalse(detalle.getCampo().toLowerCase().contains("contrase"));
            }
        }
    }

    @Test
    void eventosDeSesionTienenAutorFijoYEstadoDeSesion() {
        Apoderado titular = apoderado();

        RegistrarAuditoriaRequestDTO inicio = AuditoriaUsuarios.inicioDeSesion(titular);
        RegistrarAuditoriaRequestDTO fallido = AuditoriaUsuarios.intentoFallido(null);
        RegistrarAuditoriaRequestDTO cierre = AuditoriaUsuarios.cierreDeSesion(3L);

        assertTrue(inicio.isAutorFijo() && fallido.isAutorFijo() && cierre.isAutorFijo());
        assertEquals(3L, inicio.getUsuarioId());
        assertNull(fallido.getUsuarioId());
        assertEquals("Sin sesión", inicio.getDetalles().get(0).getValorAnterior());
        assertEquals("Sesión iniciada", inicio.getDetalles().get(0).getValorNuevo());
        assertEquals("Sesión iniciada", cierre.getDetalles().get(0).getValorAnterior());
        assertEquals("Sin sesión", cierre.getDetalles().get(0).getValorNuevo());
        assertEquals(TipoAccionAuditoria.LOGIN, fallido.getAccion());
    }
}
