package com.educoders.Gestion_Matriz_Horarios.presentacion.controladores;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.*;
import com.educoders.Gestion_Matriz_Horarios.servicio.BloqueHorarioService;
import com.educoders.Gestion_Matriz_Horarios.servicio.HorarioCargaMasivaService;
import com.educoders.Gestion_Matriz_Horarios.servicio.HorarioCsvService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasivaService;
import com.educoders.Infraestructura_Servicios.operaciones.TipoOperacionMasiva;
import com.educoders.Infraestructura_Servicios.operaciones.UsuarioAutenticadoResolver;
import com.educoders.Infraestructura_Servicios.operaciones.dto.IniciarOperacionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bloques-horario")
public class BloqueHorarioController {

    private final BloqueHorarioService service;
    private final HorarioCsvService csvService;
    private final HorarioCargaMasivaService cargaMasivaService;
    private final OperacionMasivaService operacionMasivaService;
    private final UsuarioAutenticadoResolver usuarioAutenticadoResolver;

    public BloqueHorarioController(BloqueHorarioService service,
                                   HorarioCsvService csvService,
                                   HorarioCargaMasivaService cargaMasivaService,
                                   OperacionMasivaService operacionMasivaService,
                                   UsuarioAutenticadoResolver usuarioAutenticadoResolver) {
        this.service = service;
        this.csvService = csvService;
        this.cargaMasivaService = cargaMasivaService;
        this.operacionMasivaService = operacionMasivaService;
        this.usuarioAutenticadoResolver = usuarioAutenticadoResolver;
    }

    @PostMapping
    public ResponseEntity<RespuestaApiResponse<BloqueHorarioResponse>> crear(
            @Valid @RequestBody CrearBloqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(service.crear(request)));
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<BloqueHorarioResponse>>> listar(
            @RequestParam(required = false) Long gradoId,
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) Long anioId,
            @RequestParam(required = false) Long docenteId) {
        List<BloqueHorarioResponse> r = service.listar(gradoId, seccionId, anioId, docenteId);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(r));
    }

    /** HU-GEMIH-08: descarga de la plantilla CSV de horarios. */
    @GetMapping("/plantilla-csv")
    public ResponseEntity<byte[]> descargarPlantilla() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-horarios.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvService.generarPlantilla());
    }

    /**
     * HU-GEMIH-09: carga masiva heredada desde un único CSV.
     *
     * @deprecated Usar /carga-masiva/preview y /carga-masiva/confirmar.
     */
    @Deprecated
    @PostMapping(value = "/carga-masiva", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaApiResponse<CargaMasivaResponse>> cargaMasiva(
            @RequestParam("file") MultipartFile archivo,
            @RequestParam(required = false) Long anioAcademicoId,
            @RequestParam(required = false) Long gradoId,
            @RequestParam(required = false) Long seccionId) {
        CargaMasivaResponse resultado = csvService.cargarMasivo(
                archivo, anioAcademicoId, gradoId, seccionId);
        HttpStatus estado = resultado.getErrores().isEmpty()
                ? HttpStatus.CREATED
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(estado)
                .body(RespuestaApiResponse.crearExito(resultado));
    }

    /** Nuevo flujo: validación y preview de varios archivos sin persistir. */
    @PostMapping(value = "/carga-masiva/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaApiResponse<PreviewCargaMasivaResponse>> previewCargaMasiva(
            @RequestParam("files") List<MultipartFile> archivos,
            @RequestParam Long anioAcademicoId) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                cargaMasivaService.preview(archivos, anioAcademicoId)));
    }

    /** Nuevo flujo: confirma la lista normalizada y crea los bloques. */
    @PostMapping("/carga-masiva/confirmar")
    public ResponseEntity<RespuestaApiResponse<ConfirmarCargaMasivaResponse>> confirmarCargaMasiva(
            @Valid @RequestBody ConfirmarCargaMasivaRequest request) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                cargaMasivaService.confirmar(request)));
    }

    /** Variante asíncrona de la confirmación de carga masiva. */
    @PostMapping("/carga-masiva/confirmar/async")
    public ResponseEntity<RespuestaApiResponse<IniciarOperacionResponse>>
    confirmarCargaMasivaAsync(
            @Valid @RequestBody ConfirmarCargaMasivaRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Long usuarioId = usuarioAutenticadoResolver.idActual();
        OperacionMasiva operacion = operacionMasivaService.iniciar(
                TipoOperacionMasiva.CONFIRMAR_HORARIOS_LOTE,
                usuarioId,
                idempotencyKey,
                progreso -> cargaMasivaService.confirmar(request, usuarioId, progreso));

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(RespuestaApiResponse.crearExito(
                        IniciarOperacionResponse.desde(operacion)));
    }

    /** Exporta una grilla de ensayo sin guardarla. */
    @PostMapping("/exportar-borrador")
    public ResponseEntity<byte[]> exportarBorrador(
            @Valid @RequestBody ExportarBorradorRequest request) {
        byte[] contenido = cargaMasivaService.exportarBorrador(request);
        return archivo(contenido, request.formato(), "horarios-borrador");
    }

    /** Exporta los bloques existentes con IDs y nombres. */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) Long anioId,
            @RequestParam(required = false) Long gradoId,
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) Long docenteId,
            @RequestParam(defaultValue = "csv") String formato) {

        byte[] contenido = cargaMasivaService.exportar(
                anioId, gradoId, seccionId, docenteId, formato);
        return archivo(contenido, formato, "horarios");
    }

    /** Plantilla nueva en CSV o XLSX. */
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla(
            @RequestParam(defaultValue = "csv") String formato) {
        byte[] contenido = cargaMasivaService.generarPlantilla(formato);
        return archivo(contenido, formato, "plantilla-horarios");
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, String formato, String prefijo) {
        boolean excel = "xlsx".equalsIgnoreCase(formato) || "xls".equalsIgnoreCase(formato);
        String extension = excel ? "xlsx" : "csv";
        MediaType tipo = excel
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv; charset=UTF-8");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + prefijo + "." + extension + "\"")
                .contentType(tipo)
                .body(contenido);
    }

    /** Guardado masivo: reemplaza el estado completo de bloques de una sección. */
    @PutMapping("/seccion/{seccionId}")
    public ResponseEntity<RespuestaApiResponse<GuardadoMasivoBloquesResponse>> guardarMasivo(
            @PathVariable Long seccionId,
            @Valid @RequestBody GuardarBloquesSeccionRequest request) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(
                service.guardarMasivo(seccionId, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<BloqueHorarioResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.obtener(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<BloqueHorarioResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarBloqueRequest request) {
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RespuestaApiResponse<Void>> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(null));
    }
}