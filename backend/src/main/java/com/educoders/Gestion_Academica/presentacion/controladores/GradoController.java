package com.educoders.Gestion_Academica.presentacion.controladores;

import com.educoders.Gestion_Academica.presentacion.dto.CrearGradoRequest;
import com.educoders.Gestion_Academica.presentacion.dto.GradoResponse;
import com.educoders.Gestion_Academica.servicio.GradoSeccionService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grados")
public class GradoController {

    private final GradoSeccionService gradoSeccionService;

    public GradoController(GradoSeccionService gradoSeccionService) {
        this.gradoSeccionService = gradoSeccionService;
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<GradoResponse>> crearGrado(@Valid @RequestBody CrearGradoRequest request) {
        GradoResponse response = gradoSeccionService.crearGrado(request);
        return new ResponseEntity<>(
                RespuestaApiResponse.crearExito(response),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<GradoResponse>>> listarGrados() {
        List<GradoResponse> response = gradoSeccionService.listarGrados();
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<GradoResponse>> obtenerGrado(@PathVariable Long id) {
        GradoResponse response = gradoSeccionService.obtenerGradoPorId(id);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(response));
    }
}