package com.educoders.Gestion_Asistencia.presentacion.controladores;

import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaDocenteResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaHistorialResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaSesionDetalleResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.GuardarAsistenciaEstudiantesRequest;
import com.educoders.Gestion_Asistencia.servicio.AsistenciaService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/asistencias")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @GetMapping("/sesiones/{sesionId}")
    public ResponseEntity<RespuestaApiResponse<AsistenciaSesionDetalleResponse>>
    obtenerDetalle(@PathVariable Long sesionId) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                asistenciaService.obtenerDetalle(sesionId)));
    }

    @PutMapping("/sesiones/{sesionId}/estudiantes")
    public ResponseEntity<RespuestaApiResponse<AsistenciaSesionDetalleResponse>>
    guardarEstudiantes(
            @PathVariable Long sesionId,
            @Valid @RequestBody GuardarAsistenciaEstudiantesRequest request) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                asistenciaService.guardarAsistenciaEstudiantes(sesionId, request)));
    }

    @PutMapping("/sesiones/{sesionId}/docente")
    public ResponseEntity<RespuestaApiResponse<AsistenciaDocenteResponse>>
    registrarDocente(@PathVariable Long sesionId) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                asistenciaService.registrarAsistenciaDocente(sesionId)));
    }

    @GetMapping("/docente/mi-asistencia")
    public ResponseEntity<RespuestaApiResponse<AsistenciaHistorialResponse>>
    historialDocente(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                asistenciaService.historialDocente(desde, hasta, cursoId, page, size)));
    }

    @GetMapping("/estudiante/mi-asistencia")
    public ResponseEntity<RespuestaApiResponse<AsistenciaHistorialResponse>>
    historialEstudiante(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                asistenciaService.historialEstudiante(desde, hasta, cursoId, page, size)));
    }

    @GetMapping("/apoderado/{estudianteId}")
    public ResponseEntity<RespuestaApiResponse<AsistenciaHistorialResponse>>
    historialApoderado(
            @PathVariable Long estudianteId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                asistenciaService.historialApoderado(
                        estudianteId, desde, hasta, cursoId, page, size)));
    }
}
