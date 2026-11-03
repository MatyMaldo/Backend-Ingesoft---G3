package com.educoders.Gestion_Matriz_Horarios.presentacion.controladores;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.*;
import com.educoders.Gestion_Matriz_Horarios.servicio.SesionClaseService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sesiones-clase")
public class SesionClaseController {

    private final SesionClaseService service;

    public SesionClaseController(SesionClaseService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    public ResponseEntity<RespuestaApiResponse<PreviewSesionesResponse>> preview(
            @Valid @RequestBody PreviewSesionesRequest request) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.previewSesiones(request)));
    }

    @PostMapping("/generar")
    public ResponseEntity<RespuestaApiResponse<GenerarSesionesResponse>> generar(
            @Valid @RequestBody GenerarSesionesRequest request) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.generarSesiones(request)));
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<SesionClaseResponse>>> listar(
            @RequestParam Long seccionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                service.listarPorSeccion(seccionId, fechaInicio, fechaFin)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<SesionClaseResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.obtener(id)));
    }
}