package com.educoders.Gestion_Asistencia.dominio;

import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "detalles_reporte_inasistencia")
public class DetalleReporteAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_falta", nullable = false)
    private LocalDate fecha;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAsistencia estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asistencia_referenciada_id", nullable = false)
    private AsistenciaEstudiante asistencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_inasistencia_id", nullable = false)
    private ReporteAsistencia reporte;

    protected DetalleReporteAsistencia() {
    }

    DetalleReporteAsistencia(ReporteAsistencia reporte, AsistenciaEstudiante asistencia) {
        this.reporte = reporte;
        this.asistencia = asistencia;
        this.fecha = asistencia.getSesionClase().getFecha();
        this.estado = asistencia.getEstado();
        this.observacion = asistencia.getObservaciones();
    }

    public LocalDate getFecha() { return fecha; }
    public String getObservacion() { return observacion; }
    public EstadoAsistencia getEstado() { return estado; }
    public AsistenciaEstudiante getAsistencia() { return asistencia; }
}
