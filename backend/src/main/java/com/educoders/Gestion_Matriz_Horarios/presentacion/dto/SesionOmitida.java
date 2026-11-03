package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import java.time.LocalDate;

public class SesionOmitida {

    private Long bloqueHorarioId;
    private LocalDate fecha;

    public SesionOmitida() {
    }

    public SesionOmitida(Long bloqueHorarioId, LocalDate fecha) {
        this.bloqueHorarioId = bloqueHorarioId;
        this.fecha = fecha;
    }

    public Long getBloqueHorarioId() { return bloqueHorarioId; }
    public void setBloqueHorarioId(Long bloqueHorarioId) { this.bloqueHorarioId = bloqueHorarioId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
}