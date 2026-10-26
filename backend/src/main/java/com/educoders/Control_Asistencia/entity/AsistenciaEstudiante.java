package com.educoders.Control_Asistencia.entity;

import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Representa el registro de asistencia de un estudiante.
 */
@Entity
@Table(name = "asistencias_estudiante")
public class AsistenciaEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsistencia estado;

    @Column(name = "sesion_clase_id", nullable = false)
    private Long sesionClaseId;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    protected AsistenciaEstudiante() {
    }

    public AsistenciaEstudiante(LocalDate fechaRegistro, EstadoAsistencia estado, Long sesionClaseId, Long estudianteId) {
        this.fechaRegistro = fechaRegistro;
        this.estado = estado;
        this.sesionClaseId = sesionClaseId;
        this.estudianteId = estudianteId;
    }

    public Long obtenerId() {
        return id;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public EstadoAsistencia obtenerEstado() {
        return estado;
    }

    public void setEstado(EstadoAsistencia estado) {
        this.estado = estado;
    }

    public Long getSesionClaseId() {
        return sesionClaseId;
    }

    public void setSesionClaseId(Long sesionClaseId) {
        this.sesionClaseId = sesionClaseId;
    }

    public Long getEstudianteId() {
        return estudianteId;
    }

    public void setEstudianteId(Long estudianteId) {
        this.estudianteId = estudianteId;
    }
}
