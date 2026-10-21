package com.educoders.Gestion_Academica_Motor_Evaluacion.dominio;

import jakarta.persistence.*;

// Importa tus entidades existentes (ajusta las rutas si es necesario)
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;

@Entity
@Table(name = "asignaciones_curso")
public class AsignacionCurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    // Descomenta esto cuando tengas la entidad Seccion
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;


    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Curso getCurso() { return curso; }
    public void setCurso(Curso curso) { this.curso = curso; }
    public Docente getDocente() { return docente; }
    public void setDocente(Docente docente) { this.docente = docente; }
    public Seccion getSeccion() { return seccion; }
    public void setSeccion(Seccion seccion) { this.seccion = seccion; }
}