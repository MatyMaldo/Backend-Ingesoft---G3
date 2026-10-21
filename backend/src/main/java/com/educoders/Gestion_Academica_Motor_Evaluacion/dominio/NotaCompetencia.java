package com.educoders.Gestion_Academica_Motor_Evaluacion.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "notas_competencia")
public class NotaCompetencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competencia_id", nullable = false)
    private Competencia competencia;

    // Getters y Setters básicos
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Competencia getCompetencia() { return competencia; }
    public void setCompetencia(Competencia competencia) { this.competencia = competencia; }
}