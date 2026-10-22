package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CrearSeccionRequest {

    @NotBlank(message = "El nombre de la sección es obligatorio")
    @Pattern(regexp = "^[A-Za-z]$", message = "El nombre de la sección debe ser una sola letra del alfabeto")
    private String nombre;

    @NotNull(message = "La capacidad máxima es obligatoria")
    @Min(value = 1, message = "La capacidad máxima debe ser mayor a 0")
    @jakarta.validation.constraints.Max(value = 65535, message = "La capacidad máxima no puede exceder 65535")
    private Integer capacidadMaxima;

    @NotNull(message = "El ID del grado es obligatorio")
    private Long gradoId;

    @NotNull(message = "El ID del año académico es obligatorio")
    private Long anioAcademicoId;

    @NotNull(message = "El ID del tutor es obligatorio")
    private Long tutorId;

    public CrearSeccionRequest() {
    }

    public CrearSeccionRequest(String nombre, Integer capacidadMaxima, Long gradoId, Long anioAcademicoId, Long tutorId) {
        this.nombre = nombre;
        this.capacidadMaxima = capacidadMaxima;
        this.gradoId = gradoId;
        this.anioAcademicoId = anioAcademicoId;
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

    public Long getGradoId() {
        return gradoId;
    }

    public void setGradoId(Long gradoId) {
        this.gradoId = gradoId;
    }

    public Long getAnioAcademicoId() {
        return anioAcademicoId;
    }

    public void setAnioAcademicoId(Long anioAcademicoId) {
        this.anioAcademicoId = anioAcademicoId;
    }

    public Long getTutorId() {
        return tutorId;
    }

    public void setTutorId(Long tutorId) {
        this.tutorId = tutorId;
    }
}