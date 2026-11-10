package com.educoders.Infraestructura_Servicios.operaciones;

import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Infraestructura_Servicios.operaciones.dto.OperacionMasivaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta del estado de las operaciones masivas iniciadas de forma asíncrona.
 */
@RestController
@RequestMapping("/api/v1/operaciones-masivas")
public class OperacionesMasivasController {

    private final OperacionMasivaService operacionMasivaService;

    public OperacionesMasivasController(OperacionMasivaService operacionMasivaService) {
        this.operacionMasivaService = operacionMasivaService;
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<RespuestaApiResponse<OperacionMasivaResponse>> obtener(
            @PathVariable String jobId) {
        OperacionMasiva operacion = operacionMasivaService.obtener(jobId);
        return ResponseEntity.ok(
                RespuestaApiResponse.crearExito(OperacionMasivaResponse.desde(operacion)));
    }
}
