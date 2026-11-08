package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;

import java.time.LocalDate;
import java.time.LocalTime;

public class SesionClaseResponse {
    private Long id;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private EstadoSesion estado;
    private Long bloqueHorarioId;
    private DiaSemana diaSemana;
    private Long aulaId;
    private String aulaNombre;
    private Long cursoId;
    private String cursoNombre;
    private Long docenteId;
    private String docenteNombre;
    private Long gradoId;
    private String gradoNombre;
    private Long seccionId;
    private String seccionNombre;

    public SesionClaseResponse() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public EstadoSesion getEstado() { return estado; }
    public void setEstado(EstadoSesion estado) { this.estado = estado; }
    public Long getBloqueHorarioId() { return bloqueHorarioId; }
    public void setBloqueHorarioId(Long bloqueHorarioId) { this.bloqueHorarioId = bloqueHorarioId; }
    public DiaSemana getDiaSemana() { return diaSemana; }
    public void setDiaSemana(DiaSemana diaSemana) { this.diaSemana = diaSemana; }
    public Long getAulaId() { return aulaId; }
    public void setAulaId(Long aulaId) { this.aulaId = aulaId; }
    public String getAulaNombre() { return aulaNombre; }
    public void setAulaNombre(String aulaNombre) { this.aulaNombre = aulaNombre; }
    public Long getCursoId() { return cursoId; }
    public void setCursoId(Long cursoId) { this.cursoId = cursoId; }
    public String getCursoNombre() { return cursoNombre; }
    public void setCursoNombre(String cursoNombre) { this.cursoNombre = cursoNombre; }
    public Long getDocenteId() { return docenteId; }
    public void setDocenteId(Long docenteId) { this.docenteId = docenteId; }
    public String getDocenteNombre() { return docenteNombre; }
    public void setDocenteNombre(String docenteNombre) { this.docenteNombre = docenteNombre; }
    public Long getGradoId() { return gradoId; }
    public void setGradoId(Long gradoId) { this.gradoId = gradoId; }
    public String getGradoNombre() { return gradoNombre; }
    public void setGradoNombre(String gradoNombre) { this.gradoNombre = gradoNombre; }
    public Long getSeccionId() { return seccionId; }
    public void setSeccionId(Long seccionId) { this.seccionId = seccionId; }
    public String getSeccionNombre() { return seccionNombre; }
    public void setSeccionNombre(String seccionNombre) { this.seccionNombre = seccionNombre; }
}