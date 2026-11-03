package com.educoders.Gestion_Matriz_Horarios.presentacion.controladores;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.*;
import com.educoders.Gestion_Matriz_Horarios.servicio.BloqueHorarioService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bloques-horario")
public class BloqueHorarioController {

    private final BloqueHorarioService service;

    public BloqueHorarioController(BloqueHorarioService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<BloqueHorarioResponse>> crear(
            @Valid @RequestBody CrearBloqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(service.crear(request)));
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<BloqueHorarioResponse>>> listar(
            @RequestParam(required = false) Long gradoId,
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) Long anioId,
            @RequestParam(required = false) Long docenteId) {
        List<BloqueHorarioResponse> r = service.listar(gradoId, seccionId, anioId, docenteId);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(r));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<BloqueHorarioResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.obtener(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<BloqueHorarioResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarBloqueRequest request) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<Void>> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(null));
    }
}