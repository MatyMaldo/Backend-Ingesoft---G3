package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

public class AulaResponse {
    private Long id;
    private String nombre;
    private Integer capacidad;

    public AulaResponse() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
}