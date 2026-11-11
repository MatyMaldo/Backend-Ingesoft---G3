package com.educoders.Seguridad_Auditoria.presentacion.controladores;

import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasivaService;
import com.educoders.Infraestructura_Servicios.operaciones.TipoOperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.UsuarioAutenticadoResolver;
import com.educoders.Infraestructura_Servicios.operaciones.dto.IniciarOperacionResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaUsuariosRequest;
import com.educoders.Seguridad_Auditoria.servicio.CargaMasivaUsuariosService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Carga masiva de usuarios desde el archivo ya revisado en pantalla. El controlador solo delega: el trabajo corre en
 * segundo plano y su avance y resultado se consultan en {@code GET /api/v1/operaciones-masivas/{jobId}}.
 */
@RestController
@RequestMapping("/api/v1/usuarios/carga-masiva")
public class CargaMasivaUsuariosController {

    private final CargaMasivaUsuariosService cargaMasivaService;
    private final OperacionMasivaService operacionMasivaService;
    private final UsuarioAutenticadoResolver usuarioAutenticadoResolver;

    public CargaMasivaUsuariosController(CargaMasivaUsuariosService cargaMasivaService,
                                         OperacionMasivaService operacionMasivaService,
                                         UsuarioAutenticadoResolver usuarioAutenticadoResolver) {
        this.cargaMasivaService = cargaMasivaService;
        this.operacionMasivaService = operacionMasivaService;
        this.usuarioAutenticadoResolver = usuarioAutenticadoResolver;
    }

    /** Registra en lote los usuarios de las filas enviadas. Una fila inválida se rechaza sin detener al resto. */
    @PostMapping("/async")
    public ResponseEntity<RespuestaApiResponse<IniciarOperacionResponse>> cargarAsync(
            @Valid @RequestBody CargaMasivaUsuariosRequest solicitud,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Long usuarioId = usuarioAutenticadoResolver.idActual();
        OperacionMasiva operacion = operacionMasivaService.iniciar(
                TipoOperacionMasiva.CARGA_USUARIOS_LOTE,
                usuarioId,
                idempotencyKey,
                progreso -> cargaMasivaService.cargar(solicitud, usuarioId, progreso));

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(RespuestaApiResponse.crearExito(IniciarOperacionResponse.desde(operacion)));
    }
}
