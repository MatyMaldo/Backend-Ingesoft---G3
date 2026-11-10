package com.educoders.Gestion_Matricula_Pagos.presentacion.controladores;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoApoderadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoPagoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoPensionRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosMatriculaLoteRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosMatriculaLoteResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosPensionLoteRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosPensionLoteResponse;
import com.educoders.Gestion_Matricula_Pagos.servicio.CompromisoPagoService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasivaService;
import com.educoders.Infraestructura_Servicios.operaciones.TipoOperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.UsuarioAutenticadoResolver;
import com.educoders.Infraestructura_Servicios.operaciones.dto.IniciarOperacionResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/compromisos-pago")
public class CompromisoPagoController {

    private final CompromisoPagoService compromisoPagoService;
    private final OperacionMasivaService operacionMasivaService;
    private final UsuarioAutenticadoResolver usuarioAutenticadoResolver;

    public CompromisoPagoController(CompromisoPagoService compromisoPagoService,
                                    OperacionMasivaService operacionMasivaService,
                                    UsuarioAutenticadoResolver usuarioAutenticadoResolver) {
        this.compromisoPagoService = compromisoPagoService;
        this.operacionMasivaService = operacionMasivaService;
        this.usuarioAutenticadoResolver = usuarioAutenticadoResolver;
    }

    @PostMapping("/matricula")
    public ResponseEntity<RespuestaApiResponse<CompromisoPagoResponse>> emitirCompromisoMatricula(
            @Valid @RequestBody EmitirCompromisoMatriculaRequest request) {

        CompromisoPagoResponse respuesta = compromisoPagoService.emitirCompromisoMatricula(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(respuesta));
    }

    @PostMapping("/matricula/lote")
    public ResponseEntity<RespuestaApiResponse<EmitirCompromisosMatriculaLoteResponse>>
    emitirCompromisosMatriculaLote(
            @Valid @RequestBody EmitirCompromisosMatriculaLoteRequest request) {

        EmitirCompromisosMatriculaLoteResponse respuesta =
                compromisoPagoService.emitirCompromisosMatriculaLote(request);

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(respuesta));
    }

    @PostMapping("/pension/lote")
    public ResponseEntity<RespuestaApiResponse<EmitirCompromisosPensionLoteResponse>>
    emitirCompromisosPensionLote(
            @Valid @RequestBody EmitirCompromisosPensionLoteRequest request) {

        EmitirCompromisosPensionLoteResponse respuesta =
                compromisoPagoService.emitirCompromisosPensionLote(request);

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(respuesta));
    }

    @PostMapping("/matricula/lote/async")
    public ResponseEntity<RespuestaApiResponse<IniciarOperacionResponse>>
    emitirCompromisosMatriculaLoteAsync(
            @Valid @RequestBody EmitirCompromisosMatriculaLoteRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Long usuarioId = usuarioAutenticadoResolver.idActual();
        OperacionMasiva operacion = operacionMasivaService.iniciar(
                TipoOperacionMasiva.EMISION_MATRICULA_LOTE,
                usuarioId,
                idempotencyKey,
                progreso -> compromisoPagoService.emitirCompromisosMatriculaLote(
                        request, usuarioId, progreso));

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(RespuestaApiResponse.crearExito(
                        IniciarOperacionResponse.desde(operacion)));
    }

    @PostMapping("/pension/lote/async")
    public ResponseEntity<RespuestaApiResponse<IniciarOperacionResponse>>
    emitirCompromisosPensionLoteAsync(
            @Valid @RequestBody EmitirCompromisosPensionLoteRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Long usuarioId = usuarioAutenticadoResolver.idActual();
        OperacionMasiva operacion = operacionMasivaService.iniciar(
                TipoOperacionMasiva.EMISION_PENSION_LOTE,
                usuarioId,
                idempotencyKey,
                progreso -> compromisoPagoService.emitirCompromisosPensionLote(
                        request, usuarioId, progreso));

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(RespuestaApiResponse.crearExito(
                        IniciarOperacionResponse.desde(operacion)));
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
            @RequestParam(required = false) Integer anio) {

        List<CompromisoApoderadoResponse> compromisos = compromisoPagoService
                .listarCompromisosPorEstudianteYAnio(estudianteId, anio);

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(compromisos));
    }
}