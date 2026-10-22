package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import java.time.LocalDate;

public class AnioAcademicoResponse {

    private Long id;
    private Integer anio;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    public AnioAcademicoResponse() {
    }

    public AnioAcademicoResponse(Long id, Integer anio, LocalDate fechaInicio, LocalDate fechaFin) {
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

    public static AnioAcademicoResponseBuilder builder() {
        return new AnioAcademicoResponseBuilder();
    }

    public static class AnioAcademicoResponseBuilder {
        private Long id;
        private Integer anio;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;

        public AnioAcademicoResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AnioAcademicoResponseBuilder anio(Integer anio) {
            this.anio = anio;
            return this;
        }

        public AnioAcademicoResponseBuilder fechaInicio(LocalDate fechaInicio) {
            this.fechaInicio = fechaInicio;
            return this;
        }

        public AnioAcademicoResponseBuilder fechaFin(LocalDate fechaFin) {
            this.fechaFin = fechaFin;
            return this;
        }

        public AnioAcademicoResponse build() {
            return new AnioAcademicoResponse(id, anio, fechaInicio, fechaFin);
        }
    }
}
