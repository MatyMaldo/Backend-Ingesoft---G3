package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class ActualizarSeccionRequest {

    @Pattern(regexp = "^[A-Za-z]$", message = "El nombre de la sección debe ser una sola letra del alfabeto")
    private String nombre;

    @Min(value = 1, message = "La capacidad máxima debe ser mayor a 0")
    @jakarta.validation.constraints.Max(value = 65535, message = "La capacidad máxima no puede exceder 65535")
    private Integer capacidadMaxima;

    private Long tutorId;

    public ActualizarSeccionRequest() {
    }

    public ActualizarSeccionRequest(String nombre, Integer capacidadMaxima, Long tutorId) {
        this.nombre = nombre;
        this.capacidadMaxima = capacidadMaxima;
        this.tutorId = tutorId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(Integer capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public Long getTutorId() {
        return tutorId;
    }

    public void setTutorId(Long tutorId) {
        this.tutorId = tutorId;
    }
}
