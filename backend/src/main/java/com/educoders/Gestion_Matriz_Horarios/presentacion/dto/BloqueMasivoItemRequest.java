package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record BloqueMasivoItemRequest(
        Long id,

        @NotNull(message = "La asignación de curso es obligatoria")
        Long asignacionCursoId,

        @NotNull(message = "El aula es obligatoria")
        Long aulaId,

        @NotNull(message = "El día de la semana es obligatorio")
        DiaSemana diaSemana,

        @NotNull(message = "La hora de inicio es obligatoria")
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaFin
) {
}
