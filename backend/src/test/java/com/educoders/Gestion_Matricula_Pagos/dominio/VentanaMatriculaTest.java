package com.educoders.Gestion_Matricula_Pagos.dominio;

import com.educoders.Infraestructura_Servicios.enums.EstadoPeriodoMatricula;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VentanaMatriculaTest {

    private PeriodoMatricula periodo(LocalDate inicio, LocalDate fin) {
        PeriodoMatricula p = new PeriodoMatricula();
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        return p;
    }

    @Test
    void antesDelInicio_soloAsignacionAbierta() {
        LocalDate hoy = LocalDate.of(2027, 1, 15);
        PeriodoMatricula p = periodo(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 31));

        assertTrue(VentanaMatricula.asignacionCursoDocenteAbierta(p, hoy));
        assertFalse(VentanaMatricula.crudBloquesHorarioAbierta(p, hoy));
        assertFalse(VentanaMatricula.generacionSesionesAbierta(p, hoy));
        assertEquals(EstadoPeriodoMatricula.SIN_INICIAR, VentanaMatricula.estado(p, hoy));
    }

    @Test
    void primerDia_soloBloquesAbiertos() {
        LocalDate hoy = LocalDate.of(2027, 2, 1);
        PeriodoMatricula p = periodo(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 31));

        assertFalse(VentanaMatricula.asignacionCursoDocenteAbierta(p, hoy));
        assertTrue(VentanaMatricula.crudBloquesHorarioAbierta(p, hoy));
        assertFalse(VentanaMatricula.generacionSesionesAbierta(p, hoy));
        assertEquals(EstadoPeriodoMatricula.VIGENTE, VentanaMatricula.estado(p, hoy));
    }

    @Test
    void ultimoDia_soloBloquesAbiertos() {
        LocalDate hoy = LocalDate.of(2027, 3, 31);
        PeriodoMatricula p = periodo(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 31));

        assertFalse(VentanaMatricula.asignacionCursoDocenteAbierta(p, hoy));
        assertTrue(VentanaMatricula.crudBloquesHorarioAbierta(p, hoy));
        assertFalse(VentanaMatricula.generacionSesionesAbierta(p, hoy));
        assertEquals(EstadoPeriodoMatricula.VIGENTE, VentanaMatricula.estado(p, hoy));
    }

    @Test
    void despuesDelFin_soloGeneracionAbierta() {
        LocalDate hoy = LocalDate.of(2027, 4, 1);
        PeriodoMatricula p = periodo(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 31));

        assertFalse(VentanaMatricula.asignacionCursoDocenteAbierta(p, hoy));
        assertFalse(VentanaMatricula.crudBloquesHorarioAbierta(p, hoy));
        assertTrue(VentanaMatricula.generacionSesionesAbierta(p, hoy));
        assertEquals(EstadoPeriodoMatricula.FINALIZADO, VentanaMatricula.estado(p, hoy));
    }

    @Test
    void periodoNulo_todoCerrado() {
        LocalDate hoy = LocalDate.of(2027, 1, 15);

        assertFalse(VentanaMatricula.asignacionCursoDocenteAbierta(null, hoy));
        assertFalse(VentanaMatricula.crudBloquesHorarioAbierta(null, hoy));
        assertFalse(VentanaMatricula.generacionSesionesAbierta(null, hoy));
        assertNull(VentanaMatricula.estado(null, hoy));
    }
}
