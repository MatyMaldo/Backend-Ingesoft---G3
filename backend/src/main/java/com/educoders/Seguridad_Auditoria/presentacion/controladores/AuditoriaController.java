package com.educoders.Seguridad_Auditoria.presentacion.controladores;

import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
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
@RequestMapping("/api/v1/auditorias")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<Page<AuditoriaResponseDTO>>> listar(
            @RequestParam(required = false) CategoriaAuditoria categoria,
            @RequestParam(required = false) TipoAccionAuditoria accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            Pageable pageable) {
        Page<AuditoriaResponseDTO> resultado = auditoriaService.listarAuditorias(categoria, accion, fechaInicio, fechaFin, pageable);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(resultado));
    }

    @GetMapping("/historial/{entidad}/{entidadId}")
    public ResponseEntity<RespuestaApiResponse<Page<AuditoriaResponseDTO>>> obtenerHistorial(
            @PathVariable String entidad,
            @PathVariable Long entidadId,
            Pageable pageable) {
        Page<AuditoriaResponseDTO> resultado = auditoriaService.obtenerHistorialPorEntidad(entidad, entidadId, pageable);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(resultado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<AuditoriaResponseDTO>> obtenerPorId(@PathVariable Long id) {
        AuditoriaResponseDTO dto = auditoriaService.obtenerPorId(id);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(dto));
    }
}