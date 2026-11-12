package com.educoders.Seguridad_Auditoria.presentacion.controladores;

import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LogErrorResponse;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping({"/api/v1/admin/logs-error", "/api/admin/logs-error"})
public class LogErrorController {

    private final LogErrorService logErrorService;

    public LogErrorController(LogErrorService logErrorService) {
        this.logErrorService = logErrorService;
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<Page<LogErrorResponse>>> listarErrores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        // El id desempata los errores registrados en el mismo segundo, para que el orden sea estable.
        Sort.Direction direccion = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direccion, "fechaHora", "id");

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LogErrorResponse> pagina = logErrorService.listarErrores(fechaInicio, fechaFin, modulo, busqueda, pageable);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(pagina));
    }
}