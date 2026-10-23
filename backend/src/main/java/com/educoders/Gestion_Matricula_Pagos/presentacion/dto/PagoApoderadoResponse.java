package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoApoderadoResponse(
        Long id,
        LocalDate fechaRegistro,
        BigDecimal montoPagado,
        String evidenciaUrl,
        EstadoPago estado,
        String motivo,
        Long compromisoId,
        Long apoderadoPagadorId
) {}