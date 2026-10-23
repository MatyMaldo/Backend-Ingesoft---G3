package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import com.educoders.Gestion_Matricula_Pagos.dominio.Pago;
import com.educoders.Infraestructura_Servicios.enums.EstadoPago;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoAdminListResponse(
        Long id,
        LocalDate fechaRegistro,
        BigDecimal montoPagado,
        EstadoPago estado,
        String evidenciaUrl,
        String motivo,
        TipoCompromiso tipoCompromiso,
        Long estudianteId,
        String estudianteNombreCompleto
) {
    public static PagoAdminListResponse fromEntity(Pago p) {
        var estudiante = p.getCompromisoPago().getMatricula().getEstudiante();
        String nombreCompleto = estudiante.getNombre() + " " +
                estudiante.getApellidoPaterno() + " " + estudiante.getApellidoMaterno();

        return new PagoAdminListResponse(
                p.getId(),
                p.getFechaRegistro(),
                p.getMontoPagado(),
                p.getEstado(),
                p.getEvidenciaUrl(),
                p.getMotivo(),
                p.getCompromisoPago().getTipoCompromiso(),
                estudiante.getId(),
                nombreCompleto
        );
    }
}