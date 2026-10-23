package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public record RegistrarPagoRequest(
        @NotNull(message = "El ID del compromiso es obligatorio")
        Long compromisoId,

        @NotNull(message = "El ID del apoderado es obligatorio")
        Long apoderadoId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal montoPagado,

        @NotNull(message = "El archivo del comprobante es obligatorio")
        MultipartFile comprobante // aqui si es archivo, puesto que el pdf llega desde el frontend
) {}
