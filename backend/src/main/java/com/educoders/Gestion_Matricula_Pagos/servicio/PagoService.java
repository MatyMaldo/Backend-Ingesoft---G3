package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.*;
import com.educoders.Infraestructura_Servicios.enums.EstadoPago;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PagoService {
    List<PagoApoderadoResponse> listarPagosPorCompromisoYApoderado(Long apoderadoId, Long compromisoId);

    PagoApoderadoResponse registrarPagoApoderado(RegistrarPagoRequest request);

    /*
        HU-GEMAP-08: Listado de pagos para admin
    */
    Page<PagoAdminListResponse> listarPagosAdmin(
            Long anioAcademicoId,
            Long gradoId,
            Long seccionId,
            Long estudianteId,
            EstadoPago estado,
            TipoCompromiso tipoCompromiso,
            Pageable pageable
    );

    /*
        HU-GEMAP-08: Detalle del pago(del listado para el admin) + validacion de pago
    */
    PagoAdminDetailResponse obtenerDetallePagoAdmin(Long pagoId);
    PagoAdminDetailResponse validarPagoAdmin(Long pagoId, ValidarPagoRequest request);
    PagoAdminDetailResponse rechazarPagoAdmin(Long pagoId, RechazarPagoRequest request);


}