package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class PreviewSesionesRequest {

    @NotNull(message = "El año académico es obligatorio")
    private Long anioAcademicoId;

    @NotNull(message = "La sección es obligatoria")
    private Long seccionId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    public PreviewSesionesRequest() {
    }

    public Long getAnioAcademicoId() { return anioAcademicoId; }
    public void setAnioAcademicoId(Long anioAcademicoId) { this.anioAcademicoId = anioAcademicoId; }
    public Long getSeccionId() { return seccionId; }
    public void setSeccionId(Long seccionId) { this.seccionId = seccionId; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
}