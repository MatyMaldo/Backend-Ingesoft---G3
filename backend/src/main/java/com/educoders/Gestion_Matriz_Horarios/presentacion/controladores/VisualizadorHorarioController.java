package com.educoders.Gestion_Matriz_Horarios.presentacion.controladores;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.HorarioSemanalResponse;
import com.educoders.Gestion_Matriz_Horarios.servicio.VisualizadorHorarioService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;


@RestController
@RequestMapping("/api/v1/visualizadores-horario")
public class VisualizadorHorarioController {
    private final VisualizadorHorarioService service;

    private final EstudianteRepository estudianteRepo;



    public VisualizadorHorarioController(
            VisualizadorHorarioService service,
            EstudianteRepository estudianteRepo) {
        this.service = service;
        this.estudianteRepo = estudianteRepo;
    }

    @GetMapping("/secciones/{seccionId}")
    public ResponseEntity<RespuestaApiResponse<HorarioSemanalResponse>> porSeccion(@PathVariable Long seccionId) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.porSeccion(seccionId)));
    }

    @GetMapping("/docentes/{docenteId}")
    public ResponseEntity<RespuestaApiResponse<HorarioSemanalResponse>> porDocente(@PathVariable Long docenteId) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.porDocente(docenteId)));
    }

    @GetMapping("/mi-horario/docente")
    public ResponseEntity<RespuestaApiResponse<HorarioSemanalResponse>> miHorarioDocente() {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.miHorarioDocente()));
    }

    @GetMapping("/mi-horario/estudiante")
    public ResponseEntity<RespuestaApiResponse<HorarioSemanalResponse>> miHorarioEstudiante() {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.miHorarioEstudiante()));
    }

    @GetMapping("/apoderado/{estudianteId}")
    public ResponseEntity<RespuestaApiResponse<HorarioSemanalResponse>> porApoderado(@PathVariable Long estudianteId) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.porApoderado(estudianteId)));
    }


    public record EstudianteHorarioOpcion(Long id, String nombreCompleto) {}

    @GetMapping("/apoderado/mis-estudiantes")
    public ResponseEntity<RespuestaApiResponse<List<EstudianteHorarioOpcion>>>
    misEstudiantes() {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Usuario)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        if (!(auth.getPrincipal() instanceof Apoderado apoderado)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Acceso exclusivo para apoderados");
        }

        var estudiantes = estudianteRepo
                .findByApoderadoIdOrderByIdAsc(apoderado.getId())
                .stream()
                .map(e -> new EstudianteHorarioOpcion(
                        e.getId(),
                        e.getNombre() + " " + e.getApellidoPaterno()))
                .toList();

        return ResponseEntity.ok(
                RespuestaApiResponse.crearExito(estudiantes));
    }

}
