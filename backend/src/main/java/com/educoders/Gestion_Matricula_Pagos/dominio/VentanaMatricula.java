package com.educoders.Gestion_Matricula_Pagos.dominio;

import com.educoders.Infraestructura_Servicios.enums.EstadoPeriodoMatricula;

import java.time.LocalDate;

/**
 * Reglas de ventana del proceso de matrícula y horarios.
 *
 * Es una clase utilitaria: no es una entidad ni se persiste. Centraliza las
 * comparaciones de fechas para que asignación, bloques, carga masiva y
 * generación de sesiones no dupliquen la lógica.
 */
public final class VentanaMatricula {

    private VentanaMatricula() {
    }

    /** Asignación curso-docente: solo antes de que inicie el periodo de matrícula. */
    public static boolean asignacionCursoDocenteAbierta(PeriodoMatricula periodo, LocalDate hoy) {
        return periodo != null
                && periodo.getFechaInicio() != null
                && hoy.isBefore(periodo.getFechaInicio());
    }

    /** Creación/edición de bloques horarios: solo dentro del periodo de matrícula. */
    public static boolean crudBloquesHorarioAbierta(PeriodoMatricula periodo, LocalDate hoy) {
        return periodo != null && periodo.estaVigente(hoy);
    }

    /** Generación de sesiones: solo después de que termine el periodo de matrícula. */
    public static boolean generacionSesionesAbierta(PeriodoMatricula periodo, LocalDate hoy) {
        return periodo != null
                && periodo.getFechaFin() != null
                && hoy.isAfter(periodo.getFechaFin());
    }

    public static EstadoPeriodoMatricula estado(PeriodoMatricula periodo, LocalDate hoy) {
        if (periodo == null || periodo.getFechaInicio() == null || periodo.getFechaFin() == null) {
            return null;
        }
        if (hoy.isBefore(periodo.getFechaInicio())) {
            return EstadoPeriodoMatricula.SIN_INICIAR;
        }
        if (hoy.isAfter(periodo.getFechaFin())) {
            return EstadoPeriodoMatricula.FINALIZADO;
        }
        return EstadoPeriodoMatricula.VIGENTE;
    }
}
