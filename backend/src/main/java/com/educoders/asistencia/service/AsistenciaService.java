package com.educoders.asistencia.service;

import com.educoders.asistencia.dto.AsistenciaResponse;
import com.educoders.asistencia.dto.RegistrarAsistenciaRequest;
import com.educoders.asistencia.entity.AsistenciaEstudiante;
import com.educoders.asistencia.repository.AsistenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestiona la lógica del flujo de registro y consulta de asistencias.
 */
@Service
public class AsistenciaService {

    private final AsistenciaRepository repositorioAsistencia;

    public AsistenciaService(AsistenciaRepository repositorioAsistencia) {
        this.repositorioAsistencia = repositorioAsistencia;
    }

    @Transactional(readOnly = true)
    public List<AsistenciaResponse> listarAsistencias() {
        return repositorioAsistencia.listarAsistencias()
                .stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    /**
     * Registra una asistencia validada previamente por el DTO de entrada.
     *
     * @param solicitud datos enviados desde el frontend
     * @return asistencia persistida y preparada para la respuesta REST
     */
    @Transactional
    public AsistenciaResponse registrarAsistencia(RegistrarAsistenciaRequest solicitud) {
        String nombreEstudiante = solicitud.nombreEstudiante().trim();
        AsistenciaEstudiante asistencia = new AsistenciaEstudiante(nombreEstudiante, solicitud.estado());
        AsistenciaEstudiante asistenciaGuardada = repositorioAsistencia.guardarAsistencia(asistencia);
        return convertirARespuesta(asistenciaGuardada);
    }

    private AsistenciaResponse convertirARespuesta(AsistenciaEstudiante asistencia) {
        return new AsistenciaResponse(
                asistencia.obtenerId(),
                asistencia.obtenerNombreEstudiante(),
                asistencia.obtenerEstado());
    }
}
