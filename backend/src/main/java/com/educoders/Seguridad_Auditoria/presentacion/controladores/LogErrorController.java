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
@RequestMapping("/api/admin/logs-error")
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "fechaHora") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LogErrorResponse> pagina = logErrorService.listarErrores(fechaInicio, fechaFin, modulo, pageable);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(pagina));
    }
}