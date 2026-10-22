package com.educoders.Gestion_Matricula_Pagos.dominio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "periodos_matricula")
public class PeriodoMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anio_academico_id", nullable = false, unique = true)
    private AnioAcademico anioAcademico;

    public PeriodoMatricula() {}

    public PeriodoMatricula(Long id, LocalDate fechaInicio, LocalDate fechaFin, AnioAcademico anioAcademico) {
        this.id = id;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.anioAcademico = anioAcademico;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public AnioAcademico getAnioAcademico() { return anioAcademico; }
    public void setAnioAcademico(AnioAcademico anioAcademico) { this.anioAcademico = anioAcademico; }

    /**
     * Regla de dominio: evalúa si una fecha dada se encuentra dentro del rango de matrícula.
     */
    public boolean estaVigente(LocalDate fechaConsulta) {
        if (fechaConsulta == null || fechaInicio == null || fechaFin == null) return false;
        return !fechaConsulta.isBefore(fechaInicio) && !fechaConsulta.isAfter(fechaFin);
    }

    public static PeriodoMatriculaBuilder builder() { return new PeriodoMatriculaBuilder(); }

    public static class PeriodoMatriculaBuilder {
        private Long id;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private AnioAcademico anioAcademico;

        public PeriodoMatriculaBuilder id(Long id) { this.id = id; return this; }
        public PeriodoMatriculaBuilder fechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; return this; }
        public PeriodoMatriculaBuilder fechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; return this; }
        public PeriodoMatriculaBuilder anioAcademico(AnioAcademico anioAcademico) { this.anioAcademico = anioAcademico; return this; }
        public PeriodoMatricula build() { return new PeriodoMatricula(id, fechaInicio, fechaFin, anioAcademico); }
    }
}