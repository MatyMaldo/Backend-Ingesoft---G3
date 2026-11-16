package com.educoders.Gestion_Asistencia.dominio;

import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reportes_inasistencias")
public class ReporteAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_emisor_id", nullable = false)
    private Docente docente;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "rango_inicio", nullable = false)
    private LocalDate desde;

    @Column(name = "rango_fin", nullable = false)
    private LocalDate hasta;

    @Column(name = "comentario", length = 2000)
    private String comentarios;

    @OneToMany(mappedBy = "reporte", cascade = CascadeType.PERSIST)
    @OrderBy("fecha ASC, id ASC")
    private List<DetalleReporteAsistencia> faltas = new ArrayList<>();

    protected ReporteAsistencia() {
    }

    public ReporteAsistencia(Estudiante estudiante, Docente docente, LocalDate fechaEmision,
                             LocalDate desde, LocalDate hasta, String comentarios,
                             List<AsistenciaEstudiante> asistencias) {
        this.estudiante = estudiante;
        this.docente = docente;
        this.fechaEmision = fechaEmision;
        this.desde = desde;
        this.hasta = hasta;
        this.comentarios = comentarios;
        asistencias.forEach(asistencia -> faltas.add(new DetalleReporteAsistencia(this, asistencia)));
    }

    public Long getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public Docente getDocente() { return docente; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
    public String getComentarios() { return comentarios; }
    public List<DetalleReporteAsistencia> getFaltas() { return List.copyOf(faltas); }
}
