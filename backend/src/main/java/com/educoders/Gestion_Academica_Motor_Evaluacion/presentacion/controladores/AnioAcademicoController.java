package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.AnioAcademicoResponse;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/anios-academicos")
public class AnioAcademicoController {

    private final com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.AnioAcademicoService anioAcademicoService;

    public AnioAcademicoController(com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.AnioAcademicoService anioAcademicoService) {
        this.anioAcademicoService = anioAcademicoService;
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<AnioAcademicoResponse>>> listarAniosAcademicos() {
        List<AnioAcademicoResponse> response = anioAcademicoService.listarAniosAcademicos();
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(response));
    }
}
