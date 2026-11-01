package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.NotaResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.RegistrarNotaRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.NotaService;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/evaluaciones/{idEvaluacion}/notas")
public class NotaController {

    private final NotaService notaService;

    public NotaController(NotaService notaService) {
        this.notaService = notaService;
    }

    @PostMapping
    public ResponseEntity<NotaResponse> registrarNota(
            @PathVariable Long idEvaluacion,
            @Valid @RequestBody RegistrarNotaRequest request) {
        Docente docenteLogueado = (Docente) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        NotaResponse response = notaService.registrarOActualizarNota(idEvaluacion, request, docenteLogueado);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
