package com.educoders.Gestion_Asistencia.dominio;

import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "asistencias_estudiante",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_asistencia_estudiante_sesion",
                columnNames = {"sesion_clase_id", "estudiante_id"}))
public class AsistenciaEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora_registro", nullable = false)
    private LocalDateTime fechaHoraRegistro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsistencia estado;

    @Column(length = 1000)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_clase_id", nullable = false)
    private SesionClase sesionClase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFechaHoraRegistro() { return fechaHoraRegistro; }
    public void setFechaHoraRegistro(LocalDateTime fechaHoraRegistro) { this.fechaHoraRegistro = fechaHoraRegistro; }

    public EstadoAsistencia getEstado() { return estado; }
    public void setEstado(EstadoAsistencia estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public SesionClase getSesionClase() { return sesionClase; }
    public void setSesionClase(SesionClase sesionClase) { this.sesionClase = sesionClase; }

    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }
}
