package com.educoders.Gestion_Asistencia.presentacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record GenerarReporteAsistenciaRequest(
        @NotNull LocalDate desde,
        @NotNull LocalDate hasta,
        @NotBlank(message = "Los comentarios son obligatorios")
        @Size(max = 2000) String comentarios) {
}
