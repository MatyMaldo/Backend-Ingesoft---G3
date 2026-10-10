package com.educoders.Seguridad_Auditoria.presentacion.controladores;

import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarApoderadoRequest;
import com.educoders.Seguridad_Auditoria.servicio.ApoderadoService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios/apoderados")
public class ApoderadoController {

    private final ApoderadoService apoderadoService;

    public ApoderadoController(ApoderadoService apoderadoService) {
        this.apoderadoService = apoderadoService;
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