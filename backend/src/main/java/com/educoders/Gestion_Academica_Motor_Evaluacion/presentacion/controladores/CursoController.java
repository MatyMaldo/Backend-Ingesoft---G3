package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ActualizarCursoRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearCursoRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CursoResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.CursoService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<CursoResponse>>> listar() {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(cursoService.listarTodos()));
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<CursoResponse>> crear(
            @Valid @RequestBody CrearCursoRequest request) {
        CursoResponse response = cursoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(response));
    }

    @PutMapping("/{idCurso}")
    public ResponseEntity<RespuestaApiResponse<CursoResponse>> actualizar(
            @PathVariable Long idCurso,
            @Valid @RequestBody ActualizarCursoRequest request) {
        CursoResponse response = cursoService.actualizar(idCurso, request);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(response));
    }

    @DeleteMapping("/{idCurso}")
    public ResponseEntity<RespuestaApiResponse<Void>> eliminar(@PathVariable Long idCurso) {
        cursoService.eliminar(idCurso);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(null));
    }
}
