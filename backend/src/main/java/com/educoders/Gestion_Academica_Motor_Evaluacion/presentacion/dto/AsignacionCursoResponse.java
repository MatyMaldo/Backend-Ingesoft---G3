package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

public class AsignacionCursoResponse {

    private Long id;
    private Long cursoId;
    private String cursoNombre;
    private Long docenteId;
    private String docenteNombre;
    private Long seccionId;
    private String seccionNombre;
    private Long gradoId;
    private String gradoNombre;
    private Integer anio;

    public AsignacionCursoResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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