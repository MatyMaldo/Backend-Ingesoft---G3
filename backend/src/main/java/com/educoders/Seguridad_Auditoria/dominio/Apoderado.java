package com.educoders.Seguridad_Auditoria.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Apoderado registrado en el sistema EduCoders.
 */
@Entity
@Table(name = "apoderados")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("APODERADO")
public class Apoderado extends Usuario {

    @Column(name = "edad", nullable = false)
    private Integer edad;

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }
}