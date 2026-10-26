package com.educoders.Seguridad_Auditoria.presentacion.controladores;

import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.presentacion.dto.AuditoriaResponseDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auditorias")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<Page<AuditoriaResponseDTO>>> listar(
            @RequestParam(required = false) TipoAccionAuditoria accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            Pageable pageable) {
        Page<AuditoriaResponseDTO> resultado = auditoriaService.listarAuditorias(accion, fechaInicio, fechaFin, pageable);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(resultado));
    }

    @GetMapping("/historial/{entidad}/{entidadId}")
    public ResponseEntity<RespuestaApiResponse<List<AuditoriaResponseDTO>>> obtenerHistorial(
            @PathVariable String entidad,
            @PathVariable Long entidadId) {
        List<AuditoriaResponseDTO> resultado = auditoriaService.obtenerHistorialPorEntidad(entidad, entidadId);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(resultado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<AuditoriaResponseDTO>> obtenerPorId(@PathVariable Long id) {
        AuditoriaResponseDTO dto = auditoriaService.obtenerPorId(id);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(dto));
    }
}