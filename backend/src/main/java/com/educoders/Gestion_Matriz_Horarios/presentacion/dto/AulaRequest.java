package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AulaRequest {
    @NotBlank(message = "El nombre del aula es obligatorio")
    private String nombre;
    @NotNull(message = "La capacidad es obligatoria")
    private Integer capacidad;

    public AulaRequest() {}
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
}