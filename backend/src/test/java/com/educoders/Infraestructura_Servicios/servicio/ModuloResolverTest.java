package com.educoders.Infraestructura_Servicios.servicio;

import com.educoders.Infraestructura_Servicios.enums.Modulo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuloResolverTest {

    private final ModuloResolver resolver = new ModuloResolver();

    @Test
    void resuelveHorariosConVersion() {
        assertEquals(Modulo.HORARIOS, resolver.resolver("/api/v1/bloques-horario"));
        assertEquals(Modulo.HORARIOS, resolver.resolver("/api/v1/sesiones-clase"));
        assertEquals(Modulo.HORARIOS, resolver.resolver("/api/v1/aulas"));
    }

    @Test
    void resuelveHorariosSinVersion() {
        assertEquals(Modulo.HORARIOS, resolver.resolver("/api/sesiones-clase"));
    }

    @Test
    void resuelveHorariosConOtraVersion() {
        assertEquals(Modulo.HORARIOS, resolver.resolver("/api/v2/bloques-horario"));
    }

    @Test
    void resuelveMatriculaSinVersion() {
        assertEquals(Modulo.MATRICULA, resolver.resolver("/api/matriculas"));
    }

    @Test
    void resuelvePagos() {
        assertEquals(Modulo.PAGOS, resolver.resolver("/api/v1/pagos"));
        assertEquals(Modulo.PAGOS, resolver.resolver("/api/v1/compromisos-pago"));
    }

    @Test
    void resuelveNotasAntesQueUsuarios() {
        assertEquals(Modulo.NOTAS, resolver.resolver("/api/v1/estudiantes/5/libretas"));
        assertEquals(Modulo.NOTAS, resolver.resolver("/api/v1/estudiantes/5/revision-notas"));
        assertEquals(Modulo.NOTAS, resolver.resolver("/api/v1/evaluaciones/3/notas"));
    }

    @Test
    void resuelveUsuarios() {
        assertEquals(Modulo.USUARIOS, resolver.resolver("/api/v1/usuarios"));
        assertEquals(Modulo.USUARIOS, resolver.resolver("/api/v1/usuarios/docentes"));
    }

    @Test
    void resuelveAutenticacion() {
        assertEquals(Modulo.AUTENTICACION, resolver.resolver("/api/v1/auth/login"));
    }

    @Test
    void resuelveEstructuraAcademica() {
        assertEquals(Modulo.ESTRUCTURA_ACADEMICA, resolver.resolver("/api/v1/secciones"));
        assertEquals(Modulo.ESTRUCTURA_ACADEMICA, resolver.resolver("/api/v1/grados"));
    }

    @Test
    void rutaDesconocidaDevuelveOtros() {
        assertEquals(Modulo.OTROS, resolver.resolver("/api/v1/desconocido"));
        assertEquals(Modulo.OTROS, resolver.resolver(null));
        assertEquals(Modulo.OTROS, resolver.resolver(""));
    }
}