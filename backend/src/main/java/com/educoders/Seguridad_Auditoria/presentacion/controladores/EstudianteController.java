
package com.educoders.Seguridad_Auditoria.presentacion.controladores;

import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaEstudiantesResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.EstudianteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarEstudianteRequest;
import com.educoders.Seguridad_Auditoria.servicio.CargaMasivaEstudiantesService;
import com.educoders.Seguridad_Auditoria.servicio.EstudianteService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador de registro individual y masivo de estudiantes.
 * HU-GEMAP-02.
 */
@RestController
@RequestMapping("/api/v1/usuarios/estudiantes")
public class EstudianteController {

    private final EstudianteService estudianteService;
    private final CargaMasivaEstudiantesService cargaMasivaService;

    public EstudianteController(
            EstudianteService estudianteService,
            CargaMasivaEstudiantesService cargaMasivaService) {

        this.estudianteService = estudianteService;
        this.cargaMasivaService = cargaMasivaService;
    }

    /**
     * Registro individual de estudiantes.
     */
    @PostMapping
    public ResponseEntity<RespuestaApiResponse<EstudianteResponse>> registrar(
            @Valid @RequestBody RegistrarEstudianteRequest solicitud) {

        EstudianteResponse response =
                estudianteService.registrarEstudiante(solicitud);

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/usuarios/estudiantes/" + response.id()
                ))
                .body(RespuestaApiResponse.crearExito(response));
    }

    /**
     * Carga masiva transaccional mediante CSV.
     */
    @PostMapping(
            value = "/carga-masiva",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<RespuestaApiResponse<CargaMasivaEstudiantesResponse>>
    cargarMasivamente(
            @RequestParam("archivo") MultipartFile archivo) {

        CargaMasivaEstudiantesResponse resultado =
                cargaMasivaService.cargarEstudiantes(archivo);

        return ResponseEntity.ok(
                RespuestaApiResponse.crearExito(resultado)
        );
    }
}
