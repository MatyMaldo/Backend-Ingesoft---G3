package com.educoders.Seguridad_Auditoria.presentacion.controladores;


import com.educoders.Seguridad_Auditoria.presentacion.dto.DocenteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarDocenteRequest;
import com.educoders.Seguridad_Auditoria.servicio.DocenteService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST asociados a HU-SAT-01.
 * El controlador no contiene lógica de negocio: solo delega en el servicio.
 */
@RestController
@RequestMapping("/api/v1/usuarios/docentes")
public class DocenteController {

    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    /** HU-SAT-01: alta de docente. */
    @PostMapping
    public ResponseEntity<RespuestaApiResponse<DocenteResponse>> registrar(
            @Valid @RequestBody RegistrarDocenteRequest solicitud) {
        DocenteResponse response = docenteService.registrarDocente(solicitud);
        return ResponseEntity
                .created(URI.create("/api/v1/usuarios/" + response.id()))
                .body(RespuestaApiResponse.crearExito(response));
    }
}
