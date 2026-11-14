package com.educoders.Seguridad_Auditoria.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

/**
 * Apoderado registrado en el sistema EduCoders.
 * Se guarda su fecha de nacimiento; la edad es un valor calculado (no se persiste).
 */
@Entity
@Table(name = "apoderados")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("APODERADO")
public class Apoderado extends Usuario {

    private static final ZoneId ZONA_COLEGIO = ZoneId.of("America/Lima");

    @Column(name = "fecha_nacimiento", nullable = true)
    private LocalDate fechaNacimiento;

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    /** Edad en años cumplidos hoy, o {@code null} si no hay fecha de nacimiento. Solo informativa. */
    public Integer getEdad() {
        if (fechaNacimiento == null) {
            return null;
        }
        return Period.between(fechaNacimiento, LocalDate.now(ZONA_COLEGIO)).getYears();
    }
}
