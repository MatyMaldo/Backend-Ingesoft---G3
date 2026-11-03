package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class ActualizarBloqueRequest {
    @NotNull(message = "La asignación de curso es obligatoria")
    private Long asignacionCursoId;
    @NotNull(message = "El aula es obligatoria")
    private Long aulaId;
    @NotNull(message = "El día de la semana es obligatorio")
    private DiaSemana diaSemana;
    @NotNull(message = "La hora de inicio es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;
    @NotNull(message = "La hora de fin es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    public ActualizarBloqueRequest() {}
    public Long getAsignacionCursoId() { return asignacionCursoId; }
    public void setAsignacionCursoId(Long v) { this.asignacionCursoId = v; }
    public Long getAulaId() { return aulaId; }
    public void setAulaId(Long v) { this.aulaId = v; }
    public DiaSemana getDiaSemana() { return diaSemana; }
    public void setDiaSemana(DiaSemana v) { this.diaSemana = v; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime v) { this.horaInicio = v; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime v) { this.horaFin = v; }
}