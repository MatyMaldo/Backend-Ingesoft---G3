
package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.NotasCompetenciasResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.RegistrarNotasCompetenciasRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.NotasCompetenciasService;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador REST para registrar calificaciones
 * finales de competencias. HU-EVAL-07.
 */
@RestController
@RequestMapping("/api/v1/notas-competencias")
public class NotasCompetenciasController {

    private final NotasCompetenciasService notasCompetenciasService;

    public NotasCompetenciasController(
            NotasCompetenciasService notasCompetenciasService) {
        this.notasCompetenciasService = notasCompetenciasService;
    }

    /**
     * Registra o actualiza las notas finales de competencias.
     */
    @PostMapping
    public ResponseEntity<NotasCompetenciasResponse> registrarNotas(
            @Valid @RequestBody RegistrarNotasCompetenciasRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (!(usuario instanceof Docente docente)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo los docentes pueden registrar notas de competencias."
            );
        }

        NotasCompetenciasResponse response =
                notasCompetenciasService.registrarNotas(request, docente);

        return ResponseEntity.ok(response);
    }
}
