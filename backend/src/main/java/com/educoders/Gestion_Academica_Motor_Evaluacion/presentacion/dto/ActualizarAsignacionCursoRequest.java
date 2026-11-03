package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.NotNull;

public class ActualizarAsignacionCursoRequest {

    @NotNull(message = "El curso es obligatorio")
    private Long cursoId;

    @NotNull(message = "El docente es obligatorio")
    private Long docenteId;

    public ActualizarAsignacionCursoRequest() {
    }

    public Long getCursoId() { return cursoId; }
    public void setCursoId(Long cursoId) { this.cursoId = cursoId; }
    public Long getDocenteId() { return docenteId; }
    public void setDocenteId(Long docenteId) { this.docenteId = docenteId; }
}