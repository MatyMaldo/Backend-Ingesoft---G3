package com.educoders.Gestion_Matriz_Horarios.presentacion.controladores;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.*;
import com.educoders.Gestion_Matriz_Horarios.servicio.SesionClaseService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasivaService;
import com.educoders.Infraestructura_Servicios.operaciones.TipoOperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.UsuarioAutenticadoResolver;
import com.educoders.Infraestructura_Servicios.operaciones.dto.IniciarOperacionResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sesiones-clase")
public class SesionClaseController {

    private final SesionClaseService service;
    private final OperacionMasivaService operacionMasivaService;
    private final UsuarioAutenticadoResolver usuarioAutenticadoResolver;

    public SesionClaseController(SesionClaseService service,
                                 OperacionMasivaService operacionMasivaService,
                                 UsuarioAutenticadoResolver usuarioAutenticadoResolver) {
        this.service = service;
        this.operacionMasivaService = operacionMasivaService;
        this.usuarioAutenticadoResolver = usuarioAutenticadoResolver;
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

    @PostMapping("/generar/async")
    public ResponseEntity<RespuestaApiResponse<IniciarOperacionResponse>> generarAsync(
            @Valid @RequestBody GenerarSesionesRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Long usuarioId = usuarioAutenticadoResolver.idActual();
        OperacionMasiva operacion = operacionMasivaService.iniciar(
                TipoOperacionMasiva.GENERACION_SESIONES,
                usuarioId,
                idempotencyKey,
                progreso -> service.generarSesiones(request, progreso));

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(RespuestaApiResponse.crearExito(
                        IniciarOperacionResponse.desde(operacion)));
    }

    @PatchMapping("/{id}/omision")
    public ResponseEntity<RespuestaApiResponse<SesionClaseResponse>> cambiarOmision(
            @PathVariable Long id,
            @Valid @RequestBody CambiarOmisionRequest request) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                service.cambiarOmision(id, request.omitida())));
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<SesionClaseResponse>>> listar(
            @RequestParam Long seccionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                service.listarPorSeccion(seccionId, fechaInicio, fechaFin)));
    }

    @GetMapping("/docente/{docenteId}")
    public ResponseEntity<RespuestaApiResponse<List<SesionClaseResponse>>> listarPorDocente(
            @PathVariable Long docenteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                service.listarPorDocente(docenteId, fechaInicio, fechaFin)));
    }

    @GetMapping("/mi-horario/docente")
    public ResponseEntity<RespuestaApiResponse<List<SesionClaseResponse>>> miHorarioDocente(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                service.listarMiHorarioDocente(fechaInicio, fechaFin)));
    }

    @GetMapping("/mi-horario/estudiante")
    public ResponseEntity<RespuestaApiResponse<List<SesionClaseResponse>>> miHorarioEstudiante(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                service.listarMiHorarioEstudiante(fechaInicio, fechaFin)));
    }

    @GetMapping("/apoderado/{estudianteId}")
    public ResponseEntity<RespuestaApiResponse<List<SesionClaseResponse>>> listarPorApoderado(
            @PathVariable Long estudianteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                service.listarPorApoderado(estudianteId, fechaInicio, fechaFin)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<SesionClaseResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.obtener(id)));
    }
}