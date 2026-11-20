package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ContextoTutorLibretasResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.LibretaNotasService;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para endpoints de libretas no anidados a estudiantes.
 */
@RestController
@RequestMapping("/api/v1/libretas")
public class LibretaController {

    private final LibretaNotasService libretaNotasService;

    public LibretaController(LibretaNotasService libretaNotasService) {
        this.libretaNotasService = libretaNotasService;
    }

    /**
     * HU-EVAL-06: Obtiene el contexto del tutor para generación de libretas.
     * Incluye estudiantes de sus secciones, bimestres finalizados
     * y estado de las libretas por estudiante y bimestre.
     */
    @GetMapping("/contexto-tutor")
    public ResponseEntity<ContextoTutorLibretasResponse> obtenerContextoTutor(
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        ContextoTutorLibretasResponse response =
                libretaNotasService.obtenerContextoTutor(usuarioAutenticado);

        return ResponseEntity.ok(response);
    }
}
