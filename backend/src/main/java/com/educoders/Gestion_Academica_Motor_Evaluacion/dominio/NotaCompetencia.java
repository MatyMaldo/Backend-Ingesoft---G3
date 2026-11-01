package com.educoders.Gestion_Academica_Motor_Evaluacion.dominio;

import com.educoders.Infraestructura_Servicios.enums.EscalaNota;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "notas_competencia",
        uniqueConstraints = @UniqueConstraint(columnNames = {"estudiante_id", "bimestre_id", "competencia_id"}))
public class NotaCompetencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscalaNota valor;

    @Column(nullable = false)
    private LocalDate fechaRegistro;

    @Column(length = 1000)
    private String descripcion;

    @Column(length = 2000)
    private String conclusionDescriptiva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bimestre_id", nullable = false)
    private Bimestre bimestre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competencia_id", nullable = false)
    private Competencia competencia;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EscalaNota getValor() { return valor; }
    public void setValor(EscalaNota valor) { this.valor = valor; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getConclusionDescriptiva() { return conclusionDescriptiva; }
    public void setConclusionDescriptiva(String conclusionDescriptiva) { this.conclusionDescriptiva = conclusionDescriptiva; }

    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }

    public Bimestre getBimestre() { return bimestre; }
    public void setBimestre(Bimestre bimestre) { this.bimestre = bimestre; }

    public Competencia getCompetencia() { return competencia; }
    public void setCompetencia(Competencia competencia) { this.competencia = competencia; }
}