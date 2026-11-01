package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.EvaluacionService;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionResponse;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluaciones")
public class EvaluacionController {

    private final EvaluacionService evaluacionService;

    public EvaluacionController(EvaluacionService evaluacionService) {
        this.evaluacionService = evaluacionService;
    }

    @PostMapping
    public ResponseEntity<EvaluacionResponse> crearEvaluacion(@Valid @RequestBody EvaluacionRequest request) {
        Docente docenteLogueado = (Docente) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        EvaluacionResponse response = evaluacionService.crearEvaluacion(request, docenteLogueado);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}