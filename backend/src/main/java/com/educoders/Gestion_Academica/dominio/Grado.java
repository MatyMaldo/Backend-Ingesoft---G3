package com.educoders.Gestion_Academica.dominio;

import jakarta.persistence.*;
import jakarta.persistence.*;

@Entity
@Table(name = "grados")
public class Grado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    public Grado() {
    }

    public Grado(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    // Builder manual
    public static GradoBuilder builder() {
        return new GradoBuilder();
    }

    public static class GradoBuilder {
        private Long id;
        private String nombre;

        public GradoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public GradoBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public Grado build() {
            return new Grado(id, nombre);
        }
    }
}