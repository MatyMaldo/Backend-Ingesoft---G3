package com.educoders.Gestion_Asistencia.dominio;

import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "asistencias_docente",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_asistencia_docente_sesion",
                columnNames = "sesion_clase_id"))
public class AsistenciaDocente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora_registro", nullable = false)
    private LocalDateTime fechaHoraRegistro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsistencia estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_clase_id", nullable = false)
    private SesionClase sesionClase;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFechaHoraRegistro() { return fechaHoraRegistro; }
    public void setFechaHoraRegistro(LocalDateTime fechaHoraRegistro) { this.fechaHoraRegistro = fechaHoraRegistro; }

    public EstadoAsistencia getEstado() { return estado; }
    public void setEstado(EstadoAsistencia estado) { this.estado = estado; }

    public Docente getDocente() { return docente; }
    public void setDocente(Docente docente) { this.docente = docente; }

    public SesionClase getSesionClase() { return sesionClase; }
    public void setSesionClase(SesionClase sesionClase) { this.sesionClase = sesionClase; }
}
