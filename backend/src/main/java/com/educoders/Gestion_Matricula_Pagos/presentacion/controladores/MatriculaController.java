package com.educoders.Gestion_Matricula_Pagos.presentacion.controladores;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ActualizarSeccionMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CancelacionMatriculasResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ConfirmarCargaMasivaMatriculasRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ConfirmarCargaMasivaMatriculasResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaAdminListResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculasPorEstadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.PreviewCargaMasivaMatriculasResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.SeccionEstudianteResponse;
import com.educoders.Gestion_Matricula_Pagos.servicio.MatriculaCargaMasivaService;
import com.educoders.Gestion_Matricula_Pagos.servicio.MatriculaService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/matriculas")
public class MatriculaController {

    private final MatriculaService matriculaService;
    private final MatriculaCargaMasivaService cargaMasivaService;

    public MatriculaController(
            MatriculaService matriculaService,
            MatriculaCargaMasivaService cargaMasivaService) {
        this.matriculaService = matriculaService;
        this.cargaMasivaService = cargaMasivaService;
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

    /** Cambia únicamente la sección de una matrícula PENDIENTE. */
    @PutMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<MatriculaAdminListResponse>> actualizarSeccion(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarSeccionMatriculaRequest request) {

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                matriculaService.actualizarSeccion(id, request)));
    }

    /** Elimina una matrícula PENDIENTE sin pagos asociados. */
    @DeleteMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<Void>> eliminarMatricula(@PathVariable Long id) {
        matriculaService.eliminarMatricula(id);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(null));
    }

    /** Plantilla CSV o XLSX para la carga masiva. */
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> descargarPlantilla(
            @RequestParam(defaultValue = "csv") String formato) {

        return archivo(cargaMasivaService.generarPlantilla(formato), formato, "plantilla-matriculas");
    }

    /** Exporta las matrículas PENDIENTE/CONFIRMADA del año en formato de importación. */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam Long anioId,
            @RequestParam(defaultValue = "csv") String formato) {

        return archivo(cargaMasivaService.exportar(anioId, formato), formato, "matriculas");
    }

    /** Valida el archivo sin persistir nada. */
    @PostMapping(value = "/carga-masiva/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaApiResponse<PreviewCargaMasivaMatriculasResponse>> preview(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam Long anioAcademicoId) {

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                cargaMasivaService.preview(archivo, anioAcademicoId)));
    }

    /** Confirma la carga masiva; si alguna fila falla, no se crea ninguna. */
    @PostMapping("/carga-masiva/confirmar")
    public ResponseEntity<RespuestaApiResponse<ConfirmarCargaMasivaMatriculasResponse>> confirmar(
            @Valid @RequestBody ConfirmarCargaMasivaMatriculasRequest request) {

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                cargaMasivaService.confirmar(request)));
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, String formato, String prefijo) {
        boolean excel = "xlsx".equalsIgnoreCase(formato) || "xls".equalsIgnoreCase(formato);
        String extension = excel ? "xlsx" : "csv";
        MediaType tipo = excel
                ? MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv; charset=UTF-8");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + prefijo + "." + extension + "\"")
                .contentType(tipo)
                .body(contenido);
    }
}
