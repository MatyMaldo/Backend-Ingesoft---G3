package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ActualizarSeccionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearSeccionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.SeccionResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.GradoSeccionService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST encargado de gestionar las secciones académicas.
 *
 * Permite registrar, listar, filtrar y actualizar secciones
 * mediante los servicios de gestión académica.
 */

@RestController
@RequestMapping("/api/v1/secciones")
public class SeccionController {

    private final GradoSeccionService gradoSeccionService;

    public SeccionController(GradoSeccionService gradoSeccionService) {
        this.gradoSeccionService = gradoSeccionService;
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<SeccionResponse>> crearSeccion(@Valid @RequestBody CrearSeccionRequest request) {
        SeccionResponse response = gradoSeccionService.crearSeccion(request);
        return new ResponseEntity<>(
                RespuestaApiResponse.crearExito(response),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<SeccionResponse>>> listarSecciones(
            @RequestParam(required = false) Long gradoId,
            @RequestParam(required = false) Long anioId) {

        List<SeccionResponse> response;
        if (gradoId != null && anioId != null) {
            response = gradoSeccionService.listarSeccionesPorGradoYAnio(gradoId, anioId);
        } else {
            response = gradoSeccionService.listarSecciones();
        }

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<SeccionResponse>> actualizarSeccion(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarSeccionRequest request) {
        SeccionResponse response = gradoSeccionService.actualizarSeccion(id, request);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(response));
    }
}