package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.DiaSemana;

import java.time.LocalTime;

public class BloqueHorarioResponse {
    private Long id;
    private DiaSemana diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Long aulaId;
    private String aulaNombre;
    private Long asignacionCursoId;
    private Long cursoId;
    private String cursoNombre;
    private Long docenteId;
    private String docenteNombre;
    private Long seccionId;
    private String seccionNombre;
    private Long gradoId;
    private String gradoNombre;
    private Integer anio;

    public BloqueHorarioResponse() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DiaSemana getDiaSemana() { return diaSemana; }
    public void setDiaSemana(DiaSemana diaSemana) { this.diaSemana = diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public Long getAulaId() { return aulaId; }
    public void setAulaId(Long aulaId) { this.aulaId = aulaId; }
    public String getAulaNombre() { return aulaNombre; }
    public void setAulaNombre(String aulaNombre) { this.aulaNombre = aulaNombre; }
    public Long getAsignacionCursoId() { return asignacionCursoId; }
    public void setAsignacionCursoId(Long asignacionCursoId) { this.asignacionCursoId = asignacionCursoId; }
    public Long getCursoId() { return cursoId; }
    public void setCursoId(Long cursoId) { this.cursoId = cursoId; }
    public String getCursoNombre() { return cursoNombre; }
    public void setCursoNombre(String cursoNombre) { this.cursoNombre = cursoNombre; }
    public Long getDocenteId() { return docenteId; }
    public void setDocenteId(Long docenteId) { this.docenteId = docenteId; }
    public String getDocenteNombre() { return docenteNombre; }
    public void setDocenteNombre(String docenteNombre) { this.docenteNombre = docenteNombre; }
    public Long getSeccionId() { return seccionId; }
    public void setSeccionId(Long seccionId) { this.seccionId = seccionId; }
    public String getSeccionNombre() { return seccionNombre; }
    public void setSeccionNombre(String seccionNombre) { this.seccionNombre = seccionNombre; }
    public Long getGradoId() { return gradoId; }
    public void setGradoId(Long gradoId) { this.gradoId = gradoId; }
    public String getGradoNombre() { return gradoNombre; }
    public void setGradoNombre(String gradoNombre) { this.gradoNombre = gradoNombre; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
}