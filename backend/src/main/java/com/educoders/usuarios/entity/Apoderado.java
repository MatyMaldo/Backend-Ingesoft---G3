package com.educoders.usuarios.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa a un apoderado del sistema EduCoders.
 * Hereda de Usuario y representa un rol responsable de estudiantes.
 */
@Entity
@Table(name = "apoderados")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("APODERADO")
public class Apoderado extends Usuario {

    @Column(name = "edad")
    private Integer edad;

    @OneToMany(mappedBy = "apoderado", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Estudiante> estudiantes = new ArrayList<>();

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public List<Estudiante> getEstudiantes() {
        return estudiantes;
    }

    public void setEstudiantes(List<Estudiante> estudiantes) {
        this.estudiantes = estudiantes;
    }
}
