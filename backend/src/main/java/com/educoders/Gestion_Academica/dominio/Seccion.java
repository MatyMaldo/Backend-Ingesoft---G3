package com.educoders.Gestion_Academica.dominio;

import com.educoders.Seguridad_Auditoria.dominio.Docente;
import jakarta.persistence.*;

@Entity
@Table(name = "secciones")
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(name = "capacidad_maxima", nullable = false)
    private Integer capacidadMaxima;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    private Docente tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grado_id", nullable = false)
    private Grado grado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anio_academico_id", nullable = false)
    private AnioAcademico anioAcademico;

    public Seccion() {
    }

    public Seccion(Long id, String nombre, Integer capacidadMaxima, Docente tutor, Grado grado, AnioAcademico anioAcademico) {
        this.id = id;
        this.nombre = nombre;
        this.capacidadMaxima = capacidadMaxima;
        this.tutor = tutor;
        this.grado = grado;
        this.anioAcademico = anioAcademico;
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

    public Docente getTutor() {
        return tutor;
    }

    public void setTutor(Docente tutor) {
        this.tutor = tutor;
    }

    public Grado getGrado() {
        return grado;
    }

    public void setGrado(Grado grado) {
        this.grado = grado;
    }

    public AnioAcademico getAnioAcademico() {
        return anioAcademico;
    }

    public void setAnioAcademico(AnioAcademico anioAcademico) {
        this.anioAcademico = anioAcademico;
    }

    // Builder manual
    public static SeccionBuilder builder() {
        return new SeccionBuilder();
    }

    public static class SeccionBuilder {
        private Long id;
        private String nombre;
        private Integer capacidadMaxima;
        private Docente tutor;
        private Grado grado;
        private AnioAcademico anioAcademico;

        public SeccionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public SeccionBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public SeccionBuilder capacidadMaxima(Integer capacidadMaxima) {
            this.capacidadMaxima = capacidadMaxima;
            return this;
        }

        public SeccionBuilder tutor(Docente tutor) {
            this.tutor = tutor;
            return this;
        }

        public SeccionBuilder grado(Grado grado) {
            this.grado = grado;
            return this;
        }

        public SeccionBuilder anioAcademico(AnioAcademico anioAcademico) {
            this.anioAcademico = anioAcademico;
            return this;
        }

        public Seccion build() {
            return new Seccion(id, nombre, capacidadMaxima, tutor, grado, anioAcademico);
        }
    }
}