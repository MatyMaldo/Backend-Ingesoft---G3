package com.educoders.Gestion_Matricula_Pagos.presentacion.controladores;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.PagoApoderadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarPagoRequest;
import com.educoders.Gestion_Matricula_Pagos.servicio.PagoService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping("/apoderado/compromiso/{compromisoId}")
    public ResponseEntity<RespuestaApiResponse<List<PagoApoderadoResponse>>> listarPagosPorCompromiso(
            @PathVariable Long compromisoId,
            @RequestParam Long apoderadoId) {

        List<PagoApoderadoResponse> pagos = pagoService.listarPagosPorCompromisoYApoderado(apoderadoId, compromisoId);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(pagos));
    }

    @PostMapping(value = "/apoderado/registrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaApiResponse<PagoApoderadoResponse>> registrarPago(
            @Valid @ModelAttribute RegistrarPagoRequest request) {

        PagoApoderadoResponse response = pagoService.registrarPagoApoderado(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(response));
    }
}