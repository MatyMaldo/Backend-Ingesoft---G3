package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

public class SeccionResponse {

    private Long id;
    private String nombre;
    private Integer capacidadMaxima;
    private Long gradoId;
    private String gradoNombre;
    private Long anioAcademicoId;
    private Integer anioAcademico;
    private Long tutorId;
    private String tutorNombreCompleto;

    public SeccionResponse() {
    }

    public SeccionResponse(Long id, String nombre, Integer capacidadMaxima, Long gradoId, String gradoNombre, Long anioAcademicoId, Integer anioAcademico, Long tutorId, String tutorNombreCompleto) {
        this.id = id;
        this.nombre = nombre;
        this.capacidadMaxima = capacidadMaxima;
        this.gradoId = gradoId;
        this.gradoNombre = gradoNombre;
        this.anioAcademicoId = anioAcademicoId;
        this.anioAcademico = anioAcademico;
        this.tutorId = tutorId;
        this.tutorNombreCompleto = tutorNombreCompleto;
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

    public String getGradoNombre() {
        return gradoNombre;
    }

    public void setGradoNombre(String gradoNombre) {
        this.gradoNombre = gradoNombre;
    }

    public Long getAnioAcademicoId() {
        return anioAcademicoId;
    }

    public void setAnioAcademicoId(Long anioAcademicoId) {
        this.anioAcademicoId = anioAcademicoId;
    }

    public Integer getAnioAcademico() {
        return anioAcademico;
    }

    public void setAnioAcademico(Integer anioAcademico) {
        this.anioAcademico = anioAcademico;
    }

    public Long getTutorId() {
        return tutorId;
    }

    public void setTutorId(Long tutorId) {
        this.tutorId = tutorId;
    }

    public String getTutorNombreCompleto() {
        return tutorNombreCompleto;
    }

    public void setTutorNombreCompleto(String tutorNombreCompleto) {
        this.tutorNombreCompleto = tutorNombreCompleto;
    }

    public static SeccionResponseBuilder builder() {
        return new SeccionResponseBuilder();
    }

    public static class SeccionResponseBuilder {
        private Long id;
        private String nombre;
        private Integer capacidadMaxima;
        private Long gradoId;
        private String gradoNombre;
        private Long anioAcademicoId;
        private Integer anioAcademico;
        private Long tutorId;
        private String tutorNombreCompleto;

        public SeccionResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public SeccionResponseBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public SeccionResponseBuilder capacidadMaxima(Integer capacidadMaxima) {
            this.capacidadMaxima = capacidadMaxima;
            return this;
        }

        public SeccionResponseBuilder gradoId(Long gradoId) {
            this.gradoId = gradoId;
            return this;
        }

        public SeccionResponseBuilder gradoNombre(String gradoNombre) {
            this.gradoNombre = gradoNombre;
            return this;
        }

        public SeccionResponseBuilder anioAcademicoId(Long anioAcademicoId) {
            this.anioAcademicoId = anioAcademicoId;
            return this;
        }

        public SeccionResponseBuilder anioAcademico(Integer anioAcademico) {
            this.anioAcademico = anioAcademico;
            return this;
        }

        public SeccionResponseBuilder tutorId(Long tutorId) {
            this.tutorId = tutorId;
            return this;
        }

        public SeccionResponseBuilder tutorNombreCompleto(String tutorNombreCompleto) {
            this.tutorNombreCompleto = tutorNombreCompleto;
            return this;
        }

        public SeccionResponse build() {
            return new SeccionResponse(id, nombre, capacidadMaxima, gradoId, gradoNombre, anioAcademicoId, anioAcademico, tutorId, tutorNombreCompleto);
        }
    }
}