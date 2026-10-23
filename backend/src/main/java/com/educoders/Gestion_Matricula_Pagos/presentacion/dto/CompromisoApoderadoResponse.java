package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.Mes;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompromisoApoderadoResponse(
        Long id,
        TipoCompromiso tipoCompromiso,
        Mes mes,
        LocalDate fechaEmision,
        BigDecimal montoOriginal,
        BigDecimal montoMora,
        BigDecimal montoTotal,
        EstadoCompromisoPago estadoCompromiso,
        Long matriculaId,
        boolean matriculaCancelada,
        String etiquetaMatricula,
        boolean sePuedeRegistrarPago,
        String motivoBloqueoPago
) {}