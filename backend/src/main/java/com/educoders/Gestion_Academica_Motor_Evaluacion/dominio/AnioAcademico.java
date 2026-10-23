package com.educoders.Gestion_Academica_Motor_Evaluacion.dominio;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "anios_academicos")
public class AnioAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "SMALLINT UNSIGNED")
    private Integer anio;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    public AnioAcademico() {
    }

    public AnioAcademico(Long id, Integer anio, LocalDate fechaInicio, LocalDate fechaFin) {
        this.id = id;
        this.anio = anio;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    // Builder manual
    public static AnioAcademicoBuilder builder() {
        return new AnioAcademicoBuilder();
    }

    public static class AnioAcademicoBuilder {
        private Long id;
        private Integer anio;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;

        public AnioAcademicoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AnioAcademicoBuilder anio(Integer anio) {
            this.anio = anio;
            return this;
        }

        public AnioAcademicoBuilder fechaInicio(LocalDate fechaInicio) {
            this.fechaInicio = fechaInicio;
            return this;
        }

        public AnioAcademicoBuilder fechaFin(LocalDate fechaFin) {
            this.fechaFin = fechaFin;
            return this;
        }

        public AnioAcademico build() {
            return new AnioAcademico(id, anio, fechaInicio, fechaFin);
        }
    }
}