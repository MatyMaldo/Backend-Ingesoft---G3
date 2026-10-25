package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearGradoRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.GradoResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.GradoSeccionService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.GradoConSeccionesResponse;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controlador REST encargado de gestionar los grados académicos.
 *
 * Permite registrar, listar y consultar grados mediante
 * los servicios de gestión académica.
 */

@RestController
@RequestMapping("/api/v1/grados")
public class GradoController {

    private final GradoSeccionService gradoSeccionService;

    public GradoController(GradoSeccionService gradoSeccionService) {
        this.gradoSeccionService = gradoSeccionService;
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<GradoResponse>> crearGrado(@Valid @RequestBody CrearGradoRequest request) {
        GradoResponse response = gradoSeccionService.crearGrado(request);
        return new ResponseEntity<>(
                RespuestaApiResponse.crearExito(response),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<GradoResponse>>> listarGrados() {
        List<GradoResponse> response = gradoSeccionService.listarGrados();
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<GradoResponse>> obtenerGrado(@PathVariable Long id) {
        GradoResponse response = gradoSeccionService.obtenerGradoPorId(id);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(response));
    }


    @GetMapping("/con-secciones")
    public ResponseEntity<RespuestaApiResponse<List<GradoConSeccionesResponse>>>
    listarGradosConSecciones(@RequestParam Long anioId) {

        List<GradoConSeccionesResponse> response =
                gradoSeccionService.listarGradosConSecciones(anioId);

        return ResponseEntity.ok(
                RespuestaApiResponse.crearExito(response)
        );
    }


}