package com.educoders.Gestion_Matriz_Horarios.presentacion.controladores;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.HorarioSemanalResponse;
import com.educoders.Gestion_Matriz_Horarios.servicio.VisualizadorHorarioService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visualizadores-horario")
public class VisualizadorHorarioController {
    private final VisualizadorHorarioService service;

    public VisualizadorHorarioController(VisualizadorHorarioService service) {
        this.service = service;
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
}
