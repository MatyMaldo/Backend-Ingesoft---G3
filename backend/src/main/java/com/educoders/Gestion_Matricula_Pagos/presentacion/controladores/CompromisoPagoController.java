package com.educoders.Gestion_Matricula_Pagos.presentacion.controladores;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoApoderadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoPagoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoPensionRequest;
import com.educoders.Gestion_Matricula_Pagos.servicio.CompromisoPagoService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/pension")
    public ResponseEntity<RespuestaApiResponse<CompromisoPagoResponse>> emitirCompromisoPension(
            @Valid @RequestBody EmitirCompromisoPensionRequest request) {

        CompromisoPagoResponse respuesta = compromisoPagoService.emitirCompromisoPension(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(respuesta));
    }

    @GetMapping("/apoderado/estudiante/{estudianteId}")
    public ResponseEntity<RespuestaApiResponse<List<CompromisoApoderadoResponse>>> listarCompromisosApoderado(
            @PathVariable Long estudianteId,
            @RequestParam(required = false) Integer anio,
            @RequestParam Long apoderadoId) { // Puede extraerse del token de autenticación

        List<CompromisoApoderadoResponse> compromisos = compromisoPagoService
                .listarCompromisosPorEstudianteYAnio(apoderadoId, estudianteId, anio);

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(compromisos));
    }
}