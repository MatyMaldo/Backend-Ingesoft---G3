package com.educoders.Gestion_Academica_Motor_Evaluacion.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "evaluaciones")
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String descripcion;

    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_curso_id", nullable = false)
    private AsignacionCurso asignacionCurso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bimestre_id", nullable = false)
    private Bimestre bimestre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competencia_id", nullable = false)
    private Competencia competencia;

    public Evaluacion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public AsignacionCurso getAsignacionCurso() {
        return asignacionCurso;
    }

    public void setAsignacionCurso(AsignacionCurso asignacionCurso) {
        this.asignacionCurso = asignacionCurso;
    }

    public Bimestre getBimestre() {
        return bimestre;
    }

    public void setBimestre(Bimestre bimestre) {
        this.bimestre = bimestre;
    }

    public Competencia getCompetencia() {
        return competencia;
    }

    public void setCompetencia(Competencia competencia) {
        this.competencia = competencia;
    }
}