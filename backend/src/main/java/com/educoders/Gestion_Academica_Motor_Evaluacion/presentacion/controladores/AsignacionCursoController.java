package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.*;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.AsignacionCursoService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/asignaciones-curso")
public class AsignacionCursoController {

    private final AsignacionCursoService service;

    public AsignacionCursoController(AsignacionCursoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<AsignacionCursoResponse>> crear(
            @Valid @RequestBody CrearAsignacionCursoRequest request) {
        AsignacionCursoResponse r = service.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(r));
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<AsignacionCursoResponse>>> listar(
            @RequestParam(required = false) Long gradoId,
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) Long anioId,
            @RequestParam(required = false) Long docenteId) {
        List<AsignacionCursoResponse> r = service.listar(gradoId, seccionId, anioId, docenteId);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(r));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<AsignacionCursoResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.obtener(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<AsignacionCursoResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarAsignacionCursoRequest request) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<Void>> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(null));
    }
}