package com.educoders.Gestion_Matricula_Pagos.dominio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "matriculas")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDate fechaAsignacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMatricula estado; // PENDIENTE, CONFIRMADA, CANCELADA

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    public Matricula() {}

    public Matricula(Long id, LocalDate fechaAsignacion, EstadoMatricula estado, Estudiante estudiante, Seccion seccion) {
        this.id = id;
        this.fechaAsignacion = fechaAsignacion;
        this.estado = estado;
        this.estudiante = estudiante;
        this.seccion = seccion;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDate fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }

    public EstadoMatricula getEstado() { return estado; }
    public void setEstado(EstadoMatricula estado) { this.estado = estado; }

    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }

    public Seccion getSeccion() { return seccion; }
    public void setSeccion(Seccion seccion) { this.seccion = seccion; }

    // Métodos de comportamiento de dominio
    public boolean esPendiente() { return this.estado == EstadoMatricula.PENDIENTE; }
    public boolean esConfirmada() { return this.estado == EstadoMatricula.CONFIRMADA; }
    public boolean esCancelada() { return this.estado == EstadoMatricula.CANCELADA; }

    public void confirmar() {
        this.estado = EstadoMatricula.CONFIRMADA;
    }

    public void cancelar() {
        this.estado = EstadoMatricula.CANCELADA;
    }

    public static MatriculaBuilder builder() { return new MatriculaBuilder(); }

    public static class MatriculaBuilder {
        private Long id;
        private LocalDate fechaAsignacion;
        private EstadoMatricula estado;
        private Estudiante estudiante;
        private Seccion seccion;

        public MatriculaBuilder id(Long id) { this.id = id; return this; }
        public MatriculaBuilder fechaAsignacion(LocalDate fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; return this; }
        public MatriculaBuilder estado(EstadoMatricula estado) { this.estado = estado; return this; }
        public MatriculaBuilder estudiante(Estudiante estudiante) { this.estudiante = estudiante; return this; }
        public MatriculaBuilder seccion(Seccion seccion) { this.seccion = seccion; return this; }
        public Matricula build() { return new Matricula(id, fechaAsignacion, estado, estudiante, seccion); }
    }
}