package com.educoders.config;

import com.educoders.Control_Asistencia.entity.AsistenciaEstudiante;
import com.educoders.Control_Asistencia.repository.AsistenciaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Carga datos de demostración únicamente cuando la tabla de asistencias se encuentra vacía.
 */
@Component
public class CargadorDatosIniciales {

    private final AsistenciaRepository repositorioAsistencia;

    public CargadorDatosIniciales(AsistenciaRepository repositorioAsistencia) {
        this.repositorioAsistencia = repositorioAsistencia;
    }

    @PostConstruct
    public void cargarDatosIniciales() {
        if (repositorioAsistencia.contarAsistencias() != 0) {
            return;
        }

        LocalDate fechaHoy = LocalDate.now();

        repositorioAsistencia.guardarAsistencia(
                new AsistenciaEstudiante(fechaHoy, EstadoAsistencia.PRESENTE, 1L, 1L));
        repositorioAsistencia.guardarAsistencia(
                new AsistenciaEstudiante(fechaHoy, EstadoAsistencia.TARDANZA, 1L, 2L));
        repositorioAsistencia.guardarAsistencia(
                new AsistenciaEstudiante(fechaHoy, EstadoAsistencia.FALTA, 1L, 3L));
    }
}
