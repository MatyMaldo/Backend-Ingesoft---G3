package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GenerarSesionesRequest {

    @NotNull(message = "El año académico es obligatorio")
    private Long anioAcademicoId;

    @NotNull(message = "La sección es obligatoria")
    private Long seccionId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    private List<SesionOmitida> sesionesOmitidas = new ArrayList<>();

    public GenerarSesionesRequest() {
    }

    public Long getAnioAcademicoId() { return anioAcademicoId; }
    public void setAnioAcademicoId(Long anioAcademicoId) { this.anioAcademicoId = anioAcademicoId; }
    public Long getSeccionId() { return seccionId; }
    public void setSeccionId(Long seccionId) { this.seccionId = seccionId; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public List<SesionOmitida> getSesionesOmitidas() { return sesionesOmitidas; }
    public void setSesionesOmitidas(List<SesionOmitida> sesionesOmitidas) {
        this.sesionesOmitidas = sesionesOmitidas != null ? sesionesOmitidas : new ArrayList<>();
    }
}