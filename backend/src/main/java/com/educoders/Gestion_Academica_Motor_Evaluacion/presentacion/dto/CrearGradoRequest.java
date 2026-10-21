package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.NotBlank;

public class CrearGradoRequest {

    @NotBlank(message = "El nombre del grado es obligatorio")
    private String nombre;

    public CrearGradoRequest() {
    }

    public CrearGradoRequest(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}