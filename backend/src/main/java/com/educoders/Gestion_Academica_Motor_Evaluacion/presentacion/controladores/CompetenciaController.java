package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ActualizarCompetenciaRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CompetenciaResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearCompetenciaRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.CompetenciaService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cursos/{idCurso}/competencias")
public class CompetenciaController {

    private final CompetenciaService competenciaService;

    public CompetenciaController(CompetenciaService competenciaService) {
        this.competenciaService = competenciaService;
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<CompetenciaResponse>>> listar(@PathVariable Long idCurso) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(competenciaService.listarPorCurso(idCurso)));
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<CompetenciaResponse>> crear(
            @PathVariable Long idCurso,
            @Valid @RequestBody CrearCompetenciaRequest request) {
        CompetenciaResponse response = competenciaService.crear(idCurso, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(response));
    }

    @PutMapping("/{idCompetencia}")
    public ResponseEntity<RespuestaApiResponse<CompetenciaResponse>> actualizar(
            @PathVariable Long idCurso,
            @PathVariable Long idCompetencia,
            @Valid @RequestBody ActualizarCompetenciaRequest request) {
        CompetenciaResponse response = competenciaService.actualizar(idCurso, idCompetencia, request);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(response));
    }

    @DeleteMapping("/{idCompetencia}")
    public ResponseEntity<RespuestaApiResponse<Void>> eliminar(
            @PathVariable Long idCurso,
            @PathVariable Long idCompetencia) {
        competenciaService.eliminar(idCurso, idCompetencia);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(null));
    }
}
