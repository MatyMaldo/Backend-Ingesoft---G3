package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import jakarta.validation.constraints.NotNull;

public record RegistrarMatriculaRequest(
        @NotNull(message = "El identificador del estudiante es obligatorio.")
        Long estudianteId,

        @NotNull(message = "El identificador de la sección es obligatorio.")
        Long seccionId
) {}