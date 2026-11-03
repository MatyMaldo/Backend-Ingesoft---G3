package com.educoders.Gestion_Matriz_Horarios.presentacion.controladores;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.AulaRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.AulaResponse;
import com.educoders.Gestion_Matriz_Horarios.servicio.AulaService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aulas")
public class AulaController {

    private final AulaService service;

    public AulaController(AulaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<AulaResponse>> crear(@Valid @RequestBody AulaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(service.crear(request)));
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<AulaResponse>>> listar() {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.listar()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<AulaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.obtener(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<AulaResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody AulaRequest request) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<Void>> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(null));
    }
}