package com.educoders.usuarios.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entidad que representa a un estudiante del sistema EduCoders.
 * Hereda de Usuario y representa un rol académico con código único y apoderado asociado.
 */
@Entity
@Table(name = "estudiantes")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("ESTUDIANTE")
public class Estudiante extends Usuario {

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "codigo", unique = true, nullable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apoderado_id")
    private Apoderado apoderado;

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Apoderado getApoderado() {
        return apoderado;
    }

    public void setApoderado(Apoderado apoderado) {
        this.apoderado = apoderado;
    }
}
