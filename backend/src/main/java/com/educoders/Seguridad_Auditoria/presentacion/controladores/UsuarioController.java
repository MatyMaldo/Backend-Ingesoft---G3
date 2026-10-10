package com.educoders.Seguridad_Auditoria.presentacion.controladores;

import com.educoders.Seguridad_Auditoria.presentacion.dto.ActualizarUsuarioRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.UsuarioResponse;
import com.educoders.Seguridad_Auditoria.servicio.UsuarioService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints REST asociados a HU-SAT-02.
 * El controlador no contiene lógica de negocio.
 * No existe DELETE: los usuarios no se eliminan físicamente (RT010).
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Listado paginado con filtros opcionales.
     * Ej: GET /api/v1/usuarios?tipoUsuario=DOCENTE&activo=true&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<RespuestaApiResponse<Page<UsuarioResponse>>> listar(
            @RequestParam(required = false) String tipoUsuario,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<UsuarioResponse> pagina =
                usuarioService.listar(tipoUsuario, activo, PageRequest.of(page, size));
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(pagina));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<UsuarioResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(usuarioService.obtener(id)));
    }

    /** Editar datos básicos. Solo enviar los datos a cambiar... y se identifica con el ID*/
    @PatchMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<UsuarioResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest solicitud) {
        return ResponseEntity.ok(
                RespuestaApiResponse.crearExito(usuarioService.actualizar(id, solicitud)));
    }

    /** Desactivar (no es DELETE). */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<RespuestaApiResponse<UsuarioResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(
                RespuestaApiResponse.crearExito(usuarioService.desactivar(id)));
    }

    /** Reactivar. */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<RespuestaApiResponse<UsuarioResponse>> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(
                RespuestaApiResponse.crearExito(usuarioService.reactivar(id)));
    }

    // NO hay @DeleteMapping.
}