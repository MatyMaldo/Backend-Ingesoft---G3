package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record ConfirmarCargaMasivaRequest(
        @NotNull(message = "El año académico es obligatorio.")
        Long anioAcademicoId,

        @NotNull(message = "La lista de bloques es obligatoria.")
        @Valid
        List<BloqueConfirmacion> bloques
) {
    public record BloqueConfirmacion(
            @NotNull(message = "La asignación de curso es obligatoria.")
            Long asignacionCursoId,

            @NotNull(message = "El aula es obligatoria.")
            Long aulaId,

            @NotNull(message = "El día es obligatorio.")
            DiaSemana diaSemana,

            @NotNull(message = "La hora de inicio es obligatoria.")
            @JsonFormat(pattern = "HH:mm")
            LocalTime horaInicio,

            @NotNull(message = "La hora de fin es obligatoria.")
            @JsonFormat(pattern = "HH:mm")
            LocalTime horaFin,

            String origen
    ) {
    }
}
