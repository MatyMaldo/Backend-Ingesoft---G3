package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.NotNull;

public class CrearAsignacionCursoRequest {

    @NotNull(message = "El curso es obligatorio")
    private Long cursoId;

    @NotNull(message = "El docente es obligatorio")
    private Long docenteId;

    @NotNull(message = "La sección es obligatoria")
    private Long seccionId;

    public CrearAsignacionCursoRequest() {
    }

    public Long getCursoId() { return cursoId; }
    public void setCursoId(Long cursoId) { this.cursoId = cursoId; }
    public Long getDocenteId() { return docenteId; }
    public void setDocenteId(Long docenteId) { this.docenteId = docenteId; }
    public Long getSeccionId() { return seccionId; }
    public void setSeccionId(Long seccionId) { this.seccionId = seccionId; }
}