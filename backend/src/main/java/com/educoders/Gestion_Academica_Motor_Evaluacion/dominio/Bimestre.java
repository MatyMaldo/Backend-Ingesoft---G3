package com.educoders.Gestion_Academica_Motor_Evaluacion.dominio;

import com.educoders.Infraestructura_Servicios.enums.NombreBimestre;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "bimestres")
public class Bimestre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NombreBimestre nombre; // BIM_1, BIM_2, etc.

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anio_academico_id", nullable = false)
    private AnioAcademico anioAcademico;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public NombreBimestre getNombre() { return nombre; }
    public void setNombre(NombreBimestre nombre) { this.nombre = nombre; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public AnioAcademico getAnioAcademico() { return anioAcademico; }
    public void setAnioAcademico(AnioAcademico anioAcademico) { this.anioAcademico = anioAcademico; }
}
