package com.educoders.Gestion_Matricula_Pagos.presentacion.controladores;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.*;
import com.educoders.Gestion_Matricula_Pagos.servicio.PagoService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import com.educoders.Infraestructura_Servicios.enums.EstadoPago;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping("/apoderado/compromiso/{compromisoId}")
    public ResponseEntity<RespuestaApiResponse<List<PagoApoderadoResponse>>> listarPagosPorCompromiso(
            @PathVariable Long compromisoId,
            @RequestParam Long apoderadoId) {

        List<PagoApoderadoResponse> pagos = pagoService.listarPagosPorCompromisoYApoderado(apoderadoId, compromisoId);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(pagos));
    }

    @PostMapping(value = "/apoderado/registrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaApiResponse<PagoApoderadoResponse>> registrarPago(
            @Valid @ModelAttribute RegistrarPagoRequest request) {

        PagoApoderadoResponse response = pagoService.registrarPagoApoderado(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApiResponse.crearExito(response));
    }

    @GetMapping("/admin")
    public ResponseEntity<RespuestaApiResponse<Page<PagoAdminListResponse>>> listarPagosAdmin(
            @RequestParam Long anioAcademicoId,
            @RequestParam Long gradoId,
            @RequestParam Long seccionId,
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) TipoCompromiso tipoCompromiso,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "fechaRegistro") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PagoAdminListResponse> resultado = pagoService.listarPagosAdmin(
                anioAcademicoId,
                gradoId,
                seccionId,
                estudianteId,
                estado,
                tipoCompromiso,
                pageable
        );

        return ResponseEntity.ok(RespuestaApiResponse.crearExito(resultado));
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<RespuestaApiResponse<PagoAdminDetailResponse>> obtenerDetallePagoAdmin(
            @PathVariable Long id) {

        PagoAdminDetailResponse detalle = pagoService.obtenerDetallePagoAdmin(id);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(detalle));
    }

    @PutMapping("/admin/{id}/validar")
    public ResponseEntity<RespuestaApiResponse<PagoAdminDetailResponse>> validarPagoAdmin(
            @PathVariable Long id,
            @Valid @RequestBody ValidarPagoRequest request) {

        PagoAdminDetailResponse detalleActualizado = pagoService.validarPagoAdmin(id, request);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(detalleActualizado));
    }
    @PutMapping("/admin/{id}/rechazar")
    public ResponseEntity<RespuestaApiResponse<PagoAdminDetailResponse>> rechazarPagoAdmin(
            @PathVariable Long id,
            @Valid @RequestBody RechazarPagoRequest request) {

        PagoAdminDetailResponse detalleActualizado = pagoService.rechazarPagoAdmin(id, request);
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(detalleActualizado));
    }
}