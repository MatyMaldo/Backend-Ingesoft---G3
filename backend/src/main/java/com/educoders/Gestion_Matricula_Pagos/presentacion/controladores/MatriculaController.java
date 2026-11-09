package com.educoders.Gestion_Matricula_Pagos.presentacion.controladores;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CancelacionMatriculasResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaAdminListResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculasPorEstadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.SeccionEstudianteResponse;
import com.educoders.Gestion_Matricula_Pagos.servicio.MatriculaService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matriculas")
public class MatriculaController {

    private final MatriculaService matriculaService;

    public MatriculaController(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<MatriculaResponse>> registrarMatricula(
            @Valid @RequestBody RegistrarMatriculaRequest request) {

        MatriculaResponse respuesta = matriculaService.registrarMatricula(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(respuesta));
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<Page<MatriculaAdminListResponse>>> listarMatriculas(
            @RequestParam(required = false) Long anioId,
            @RequestParam(required = false) Long gradoId,
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) EstadoMatricula estado,
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Sort sort = Sort.by(
                Sort.Order.desc("fechaAsignacion"),
                Sort.Order.desc("id")
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MatriculaAdminListResponse> resultado = matriculaService.listarMatriculas(
                anioId, gradoId, seccionId, estado, estudianteId, busqueda, pageable);

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(resultado));
    }

    /**
     * Cantidad de matrículas por estado con los filtros del listado (sin el estado): contadores de los chips.
     */
    @GetMapping("/conteo-por-estado")
    public ResponseEntity<RespuestaApiResponse<MatriculasPorEstadoResponse>> contarPorEstado(
            @RequestParam(required = false) Long anioId,
            @RequestParam(required = false) Long gradoId,
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) String busqueda) {

        MatriculasPorEstadoResponse conteo = matriculaService.contarMatriculasPorEstado(
                anioId, gradoId, seccionId, busqueda);

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(conteo));
    }

    @PostMapping("/cancelar-pendientes-por-periodo")
    public ResponseEntity<RespuestaApiResponse<CancelacionMatriculasResponse>>
    cancelarPendientesPorPeriodo() {

        int canceladas = matriculaService.cancelarPendientesPorPeriodoVencido();

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                new CancelacionMatriculasResponse(canceladas)));
    }

    @GetMapping("/estudiante/{estudianteId}/seccion")
    public ResponseEntity<RespuestaApiResponse<SeccionEstudianteResponse>> obtenerSeccionEstudiante(
            @PathVariable Long estudianteId,
            @RequestParam Long anioId) {

        SeccionEstudianteResponse respuesta =
                matriculaService.obtenerSeccionEstudiante(estudianteId, anioId);

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(respuesta));
    }
}