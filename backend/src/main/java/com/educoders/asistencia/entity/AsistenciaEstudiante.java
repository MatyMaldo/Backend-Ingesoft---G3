package com.educoders.asistencia.entity;

import com.educoders.shared.enums.EstadoAsistencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representa el registro simplificado de asistencia de un estudiante para el prototipo.
 */
@Entity
@Table(name = "asistencias_estudiantes")
public class AsistenciaEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombreEstudiante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsistencia estado;

    protected AsistenciaEstudiante() {
    }

    public AsistenciaEstudiante(String nombreEstudiante, EstadoAsistencia estado) {
        this.nombreEstudiante = nombreEstudiante;
        this.estado = estado;
    }

    public Long obtenerId() {
        return id;
    }

    public String obtenerNombreEstudiante() {
        return nombreEstudiante;
    }

    public EstadoAsistencia obtenerEstado() {
        return estado;
    }
}
