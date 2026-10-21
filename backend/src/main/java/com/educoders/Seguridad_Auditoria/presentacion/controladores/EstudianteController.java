package com.educoders.Seguridad_Auditoria.presentacion.controladores;

import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.EstudianteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarEstudianteRequest;
import com.educoders.Seguridad_Auditoria.servicio.EstudianteService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST asociados al registro de estudiantes.
 * El controlador delega la lógica de negocio al servicio.
 */
@RestController
@RequestMapping("/api/v1/usuarios/estudiantes")
public class EstudianteController {

    private final EstudianteService estudianteService;

    public EstudianteController(EstudianteService estudianteService) {
        this.estudianteService = estudianteService;
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<EstudianteResponse>> registrar(
            @Valid @RequestBody RegistrarEstudianteRequest solicitud) {

        EstudianteResponse response =
                estudianteService.registrarEstudiante(solicitud);

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/usuarios/estudiantes/" + response.id()
                ))
                .body(RespuestaApiResponse.crearExito(response));
    }
}