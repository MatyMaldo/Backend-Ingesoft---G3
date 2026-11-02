
package com.educoders.Gestion_Academica_Motor_Evaluacion.dominio;

import com.educoders.Infraestructura_Servicios.enums.EstadoLibreta;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "libretas_notas",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"estudiante_id", "bimestre_id"}))
public class LibretaNotas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_generacion")
    private LocalDate fechaGeneracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoLibreta estado = EstadoLibreta.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bimestre_id", nullable = false)
    private Bimestre bimestre;

    @Column(name = "comentarios_finales_tutor", length = 2000)
    private String comentariosFinalesTutor;

    public LibretaNotas() {
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDate fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public EstadoLibreta getEstado() {
        return estado;
    }

    public void setEstado(EstadoLibreta estado) {
        this.estado = estado;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public Bimestre getBimestre() {
        return bimestre;
    }

    public void setBimestre(Bimestre bimestre) {
        this.bimestre = bimestre;
    }

    public String getComentariosFinalesTutor() {
        return comentariosFinalesTutor;
    }

    public void setComentariosFinalesTutor(String comentariosFinalesTutor) {
        this.comentariosFinalesTutor = comentariosFinalesTutor;
    }
}
