package com.educoders.Gestion_Matriz_Horarios.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "aulas")
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private Integer capacidad;

    public Aula() {}

    public Aula(Long id, String nombre, Integer capacidad) {
        this.id = id;
        this.nombre = nombre;
        this.capacidad = capacidad;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public static AulaBuilder builder() { return new AulaBuilder(); }

    public static class AulaBuilder {
        private Long id;
        private String nombre;
        private Integer capacidad;

        public AulaBuilder id(Long id) { this.id = id; return this; }
        public AulaBuilder nombre(String nombre) { this.nombre = nombre; return this; }
        public AulaBuilder capacidad(Integer capacidad) { this.capacidad = capacidad; return this; }
        public Aula build() { return new Aula(id, nombre, capacidad); }
    }
}