package com.educoders.Seguridad_Auditoria.presentacion.controladores;

import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.EstudianteApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarApoderadoRequest;
import com.educoders.Seguridad_Auditoria.servicio.ApoderadoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios/apoderados")
public class ApoderadoController {

    private final ApoderadoService apoderadoService;

    public ApoderadoController(ApoderadoService apoderadoService) {
        this.apoderadoService = apoderadoService;
    }

    @GetMapping("/me/estudiantes")
    public ResponseEntity<RespuestaApiResponse<List<EstudianteApoderadoResponse>>> listarMisEstudiantes(
            @RequestParam(required = false) Long anioId) {

        List<EstudianteApoderadoResponse> estudiantes =
                apoderadoService.listarMisEstudiantes(anioId);

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(estudiantes));
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<ApoderadoResponse>> registrar(
            @Valid @RequestBody RegistrarApoderadoRequest solicitud) {

        ApoderadoResponse response =
                apoderadoService.registrarApoderado(solicitud);

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/usuarios/apoderados/" + response.id()
                ))
                .body(RespuestaApiResponse.crearExito(response));
    }
}