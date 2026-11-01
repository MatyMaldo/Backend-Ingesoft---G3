package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.EscalaNota;
import jakarta.validation.constraints.NotNull;

public record RegistrarNotaRequest(
        @NotNull(message = "El ID del estudiante es obligatorio")
        Long idEstudiante,

        @NotNull(message = "La nota es obligatoria. Valores válidos: AD, A, B, C")
        EscalaNota valorNota
) {}
