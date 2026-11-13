package com.educoders.Gestion_Asistencia.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public record GuardarAsistenciaEstudiantesRequest(
        @NotNull(message = "La lista de asistencias es obligatoria.")
        @Valid
        List<AsistenciaItem> asistencias
) {
    public GuardarAsistenciaEstudiantesRequest {
        asistencias = asistencias == null ? null : new ArrayList<>(asistencias);
    }

    public record AsistenciaItem(
            @NotNull(message = "El estudiante es obligatorio.")
            Long estudianteId,

            @NotNull(message = "El estado de asistencia es obligatorio.")
            EstadoAsistencia estado,

            @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres.")
            String observaciones
    ) {
    }
}
