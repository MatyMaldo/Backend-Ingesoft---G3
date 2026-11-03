package com.educoders.Gestion_Matriz_Horarios.dominio;

import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_clase")
public class SesionClase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalDateTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalDateTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoSesion estado = EstadoSesion.PROGRAMADA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bloque_horario_id", nullable = false)
    private BloqueHorario bloqueHorario;

    public SesionClase() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalDateTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalDateTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalDateTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalDateTime horaFin) { this.horaFin = horaFin; }
    public EstadoSesion getEstado() { return estado; }
    public void setEstado(EstadoSesion estado) { this.estado = estado; }
    public BloqueHorario getBloqueHorario() { return bloqueHorario; }
    public void setBloqueHorario(BloqueHorario bloqueHorario) { this.bloqueHorario = bloqueHorario; }
}