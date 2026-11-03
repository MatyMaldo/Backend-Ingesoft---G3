package com.educoders.Gestion_Matriz_Horarios.dominio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "bloques_horario")
public class BloqueHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id", nullable = false)
    private Aula aula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignacion_curso_id", nullable = false)
    private AsignacionCurso asignacionCurso;

    public BloqueHorario() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DiaSemana getDiaSemana() { return diaSemana; }
    public void setDiaSemana(DiaSemana diaSemana) { this.diaSemana = diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public Aula getAula() { return aula; }
    public void setAula(Aula aula) { this.aula = aula; }
    public AsignacionCurso getAsignacionCurso() { return asignacionCurso; }
    public void setAsignacionCurso(AsignacionCurso asignacionCurso) { this.asignacionCurso = asignacionCurso; }

    public static BloqueHorarioBuilder builder() { return new BloqueHorarioBuilder(); }

    public static class BloqueHorarioBuilder {
        private Long id;
        private DiaSemana diaSemana;
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private Aula aula;
        private AsignacionCurso asignacionCurso;

        public BloqueHorarioBuilder id(Long id) { this.id = id; return this; }
        public BloqueHorarioBuilder diaSemana(DiaSemana diaSemana) { this.diaSemana = diaSemana; return this; }
        public BloqueHorarioBuilder horaInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; return this; }
        public BloqueHorarioBuilder horaFin(LocalTime horaFin) { this.horaFin = horaFin; return this; }
        public BloqueHorarioBuilder aula(Aula aula) { this.aula = aula; return this; }
        public BloqueHorarioBuilder asignacionCurso(AsignacionCurso asignacionCurso) { this.asignacionCurso = asignacionCurso; return this; }

        public BloqueHorario build() {
            BloqueHorario b = new BloqueHorario();
            b.id = id;
            b.diaSemana = diaSemana;
            b.horaInicio = horaInicio;
            b.horaFin = horaFin;
            b.aula = aula;
            b.asignacionCurso = asignacionCurso;
            return b;
        }
    }
}