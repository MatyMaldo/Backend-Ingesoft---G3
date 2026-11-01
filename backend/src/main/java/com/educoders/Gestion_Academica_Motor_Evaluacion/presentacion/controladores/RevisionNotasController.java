package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.RevisionNotasResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.RevisionNotasService;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/estudiantes/{idEstudiante}/revision-notas")
public class RevisionNotasController {

    // HU-EVAL-03, Revisión de Notas por parte de apoderado o estudiante.
    private final RevisionNotasService revisionNotasService;

    public RevisionNotasController(RevisionNotasService revisionNotasService) {
        this.revisionNotasService = revisionNotasService;
    }

    @GetMapping("/cursos/{idCurso}")
    public ResponseEntity<RevisionNotasResponse> obtenerNotas(
            @PathVariable Long idEstudiante,
            @PathVariable Long idCurso) {

        Usuario usuarioAutenticado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        RevisionNotasResponse response = revisionNotasService.obtenerNotasEstudiante(idEstudiante, idCurso, usuarioAutenticado);
        return ResponseEntity.ok(response);
    }
}
