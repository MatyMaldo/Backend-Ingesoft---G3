package com.educoders.config;

import com.educoders.asistencia.entity.AsistenciaEstudiante;
import com.educoders.asistencia.repository.AsistenciaRepository;
import com.educoders.shared.enums.EstadoAsistencia;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

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

        repositorioAsistencia.guardarAsistencia(
                new AsistenciaEstudiante("Juan Pérez", EstadoAsistencia.PRESENTE));
        repositorioAsistencia.guardarAsistencia(
                new AsistenciaEstudiante("Ana Torres", EstadoAsistencia.TARDANZA));
        repositorioAsistencia.guardarAsistencia(
                new AsistenciaEstudiante("Luis Ramos", EstadoAsistencia.FALTA));
    }
}
