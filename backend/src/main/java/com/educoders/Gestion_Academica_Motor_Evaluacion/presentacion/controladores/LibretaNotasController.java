
package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.LibretaNotasResumenResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.LibretaNotasService;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.LibretaNotasDetalleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.List;

@RestController
@RequestMapping("/api/v1/estudiantes/{idEstudiante}/libretas")
public class LibretaNotasController {

    private final LibretaNotasService libretaNotasService;

    public LibretaNotasController(
            LibretaNotasService libretaNotasService) {

        this.libretaNotasService = libretaNotasService;
    }

    // HU-EVAL-04 y HU-EVAL-05:
    // Consultar las libretas generadas de un estudiante.
    @GetMapping
    public ResponseEntity<List<LibretaNotasResumenResponse>> listarLibretas(
            @PathVariable Long idEstudiante,
            @RequestParam Long idAnioAcademico,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        List<LibretaNotasResumenResponse> response =
                libretaNotasService.listarLibretas(
                        idEstudiante,
                        idAnioAcademico,
                        usuarioAutenticado
                );

        return ResponseEntity.ok(response);
    }

    // HU-EVAL-04 y HU-EVAL-05:
    // Consultar el detalle de una libreta generada.
    @GetMapping("/{idLibreta}")
    public ResponseEntity<LibretaNotasDetalleResponse> obtenerDetalleLibreta(
            @PathVariable Long idEstudiante,
            @PathVariable Long idLibreta,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        LibretaNotasDetalleResponse response =
                libretaNotasService.obtenerDetalleLibreta(
                        idLibreta,
                        usuarioAutenticado
                );

        // Verificar que la libreta corresponda al estudiante
        // indicado en la URL.
        if (!idEstudiante.equals(response.idEstudiante())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "La libreta no corresponde al estudiante indicado"
            );
        }

        return ResponseEntity.ok(response);
    }


    // HU-EVAL-04 y HU-EVAL-05:
    // Obtener la libreta de notas en formato PDF.
    @GetMapping(value = "/{idLibreta}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarLibretaPdf(
            @PathVariable Long idEstudiante,
            @PathVariable Long idLibreta,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        // 1. Validar permisos y consultar la libreta.
        LibretaNotasDetalleResponse detalle =
                libretaNotasService.obtenerDetalleLibreta(
                        idLibreta,
                        usuarioAutenticado
                );

        // 2. Verificar que corresponda al estudiante de la URL.
        if (!idEstudiante.equals(detalle.idEstudiante())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "La libreta no corresponde al estudiante indicado"
            );
        }

        // 3. Generar el PDF.
        byte[] pdf = libretaNotasService.descargarLibretaPdf(
                idLibreta,
                usuarioAutenticado
        );

        // 4. Devolver el PDF mediante HTTP.
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"libreta-" + idLibreta + ".pdf\""
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(pdf.length)
                .body(pdf);
    }


}
