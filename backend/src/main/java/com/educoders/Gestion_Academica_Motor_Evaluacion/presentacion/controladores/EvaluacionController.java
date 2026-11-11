package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ContextoDocenteResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.EvaluacionService;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/evaluaciones")
public class EvaluacionController {

    private final EvaluacionService evaluacionService;

    public EvaluacionController(EvaluacionService evaluacionService) {
        this.evaluacionService = evaluacionService;
    }

    @GetMapping("/contexto-docente")
    public ResponseEntity<ContextoDocenteResponse> obtenerContextoDocente() {
        Docente docenteLogueado = (Docente) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        ContextoDocenteResponse response = evaluacionService.obtenerContextoDocente(docenteLogueado);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<EvaluacionResponse>> listarEvaluaciones(
            @RequestParam(required = false) Long idAsignacionCurso,
            @RequestParam(required = false) Long idSeccion,
            @RequestParam(required = false) Long idCurso) {

        List<EvaluacionResponse> response = evaluacionService.listarEvaluaciones(
                Optional.ofNullable(idAsignacionCurso),
                Optional.ofNullable(idSeccion),
                Optional.ofNullable(idCurso)
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<EvaluacionResponse> crearEvaluacion(@Valid @RequestBody EvaluacionRequest request) {
        Docente docenteLogueado = (Docente) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        EvaluacionResponse response = evaluacionService.crearEvaluacion(request, docenteLogueado);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}