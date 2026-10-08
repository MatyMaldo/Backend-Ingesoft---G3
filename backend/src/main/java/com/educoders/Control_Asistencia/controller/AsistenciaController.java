package com.educoders.Control_Asistencia.controller;

import com.educoders.Control_Asistencia.dto.AsistenciaResponse;
import com.educoders.Control_Asistencia.dto.RegistrarAsistenciaRequest;
import com.educoders.Control_Asistencia.service.AsistenciaService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone los endpoints REST del módulo de asistencia.
 */
@RestController
@RequestMapping("/api/v1/asistencias")
public class AsistenciaController {

    private final AsistenciaService servicioAsistencia;

    public AsistenciaController(AsistenciaService servicioAsistencia) {
        this.servicioAsistencia = servicioAsistencia;
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<AsistenciaResponse>>> listarAsistencias() {
        List<AsistenciaResponse> asistencias = servicioAsistencia.listarAsistencias();
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(asistencias));
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<AsistenciaResponse>> registrarAsistencia(
            @Valid @RequestBody RegistrarAsistenciaRequest solicitud) {
        AsistenciaResponse asistencia = servicioAsistencia.registrarAsistencia(solicitud);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(asistencia));
    }
}
