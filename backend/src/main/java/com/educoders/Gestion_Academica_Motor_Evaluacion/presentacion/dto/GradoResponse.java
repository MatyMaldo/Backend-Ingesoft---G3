package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;
public class GradoResponse {

    private Long id;
    private String nombre;

    public GradoResponse() {
    }

    public GradoResponse(Long id, String nombre) {
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

    public static GradoResponseBuilder builder() {
        return new GradoResponseBuilder();
    }

    public static class GradoResponseBuilder {
        private Long id;
        private String nombre;

        public GradoResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public GradoResponseBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public GradoResponse build() {
            return new GradoResponse(id, nombre);
        }
    }
}
