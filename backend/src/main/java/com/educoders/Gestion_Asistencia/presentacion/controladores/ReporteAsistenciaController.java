package com.educoders.Gestion_Asistencia.presentacion.controladores;

import com.educoders.Gestion_Asistencia.presentacion.dto.GenerarReporteAsistenciaRequest;
import com.educoders.Gestion_Asistencia.presentacion.dto.ReporteAsistenciaResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.ReporteAsistenciaResponse.Listado;
import com.educoders.Gestion_Asistencia.presentacion.dto.ReporteAsistenciaResponse.VistaPrevia;
import com.educoders.Gestion_Asistencia.servicio.ReporteAsistenciaService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse.crearExito;

@RestController
@RequestMapping("/api/v1/asistencias/reportes")
public class ReporteAsistenciaController {

    private final ReporteAsistenciaService reportes;

    public ReporteAsistenciaController(ReporteAsistenciaService reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/docente/asignaciones/{asignacionId}/estudiantes/{estudianteId}/vista-previa")
    public RespuestaApiResponse<VistaPrevia> vistaPrevia(
            @PathVariable Long asignacionId, @PathVariable Long estudianteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return crearExito(reportes.vistaPrevia(asignacionId, estudianteId, desde, hasta));
    }

    @PostMapping("/docente/asignaciones/{asignacionId}/estudiantes/{estudianteId}")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaApiResponse<ReporteAsistenciaResponse> generar(
            @PathVariable Long asignacionId, @PathVariable Long estudianteId,
            @Valid @RequestBody GenerarReporteAsistenciaRequest request) {
        return crearExito(reportes.generar(asignacionId, estudianteId, request));
    }

    @GetMapping("/estudiante")
    public RespuestaApiResponse<Listado> listarPropios() {
        return crearExito(reportes.listarPropios());
    }

    @GetMapping("/estudiante/{reporteId}")
    public RespuestaApiResponse<ReporteAsistenciaResponse> detallePropio(@PathVariable Long reporteId) {
        return crearExito(reportes.detallePropio(reporteId));
    }

    @GetMapping("/apoderado/{estudianteId}")
    public RespuestaApiResponse<Listado> listarDeEstudiante(@PathVariable Long estudianteId) {
        return crearExito(reportes.listarDeEstudiante(estudianteId));
    }

    @GetMapping("/apoderado/{estudianteId}/{reporteId}")
    public RespuestaApiResponse<ReporteAsistenciaResponse> detalleDeEstudiante(
            @PathVariable Long estudianteId, @PathVariable Long reporteId) {
        return crearExito(reportes.detalleDeEstudiante(estudianteId, reporteId));
    }
}
