package com.educoders.Gestion_Matricula_Pagos.presentacion.controladores;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoPagoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.servicio.CompromisoPagoService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compromisos-pago")
public class CompromisoPagoController {

    private final CompromisoPagoService compromisoPagoService;

    public CompromisoPagoController(CompromisoPagoService compromisoPagoService) {
        this.compromisoPagoService = compromisoPagoService;
    }

    @PostMapping("/matricula")
    public ResponseEntity<RespuestaApiResponse<CompromisoPagoResponse>> emitirCompromisoMatricula(
            @Valid @RequestBody EmitirCompromisoMatriculaRequest request) {

        CompromisoPagoResponse respuesta = compromisoPagoService.emitirCompromisoMatricula(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(respuesta));
    }
}