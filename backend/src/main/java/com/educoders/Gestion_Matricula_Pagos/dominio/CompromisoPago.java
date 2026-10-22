package com.educoders.Gestion_Matricula_Pagos.dominio;

import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.Mes;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "compromisos_pago")
public class CompromisoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_compromiso", nullable = false, length = 20)
    private TipoCompromiso tipoCompromiso;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Mes mes; // Null para MATRICULA, obligatorio para PENSION

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "monto_original", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoOriginal;

    @Column(name = "monto_mora", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoMora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCompromisoPago estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    public CompromisoPago() {
        this.montoMora = BigDecimal.ZERO;
        this.estado = EstadoCompromisoPago.PENDIENTE;
    }

    public CompromisoPago(Long id, TipoCompromiso tipoCompromiso, Mes mes, LocalDate fechaEmision,
                          BigDecimal montoOriginal, BigDecimal montoMora, EstadoCompromisoPago estado,
                          Matricula matricula) {
        this.id = id;
        this.tipoCompromiso = tipoCompromiso;
        this.mes = mes;
        this.fechaEmision = fechaEmision;
        this.montoOriginal = montoOriginal;
        this.montoMora = montoMora != null ? montoMora : BigDecimal.ZERO;
        this.estado = estado != null ? estado : EstadoCompromisoPago.PENDIENTE;
        this.matricula = matricula;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoCompromiso getTipoCompromiso() { return tipoCompromiso; }
    public void setTipoCompromiso(TipoCompromiso tipoCompromiso) { this.tipoCompromiso = tipoCompromiso; }

    public Mes getMes() { return mes; }
    public void setMes(Mes mes) { this.mes = mes; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public BigDecimal getMontoOriginal() { return montoOriginal; }
    public void setMontoOriginal(BigDecimal montoOriginal) { this.montoOriginal = montoOriginal; }

    public BigDecimal getMontoMora() { return montoMora; }
    public void setMontoMora(BigDecimal montoMora) { this.montoMora = montoMora; }

    public EstadoCompromisoPago getEstado() { return estado; }
    public void setEstado(EstadoCompromisoPago estado) { this.estado = estado; }

    public Matricula getMatricula() { return matricula; }
    public void setMatricula(Matricula matricula) { this.matricula = matricula; }

    // Métodos de comportamiento de dominio
    public BigDecimal calcularMontoTotal() {
        return this.montoOriginal.add(this.montoMora);
    }

    public boolean esPendiente() { return this.estado == EstadoCompromisoPago.PENDIENTE; }
    public boolean esPagado() { return this.estado == EstadoCompromisoPago.PAGADO; }

    public void marcarComoPagado() {
        this.estado = EstadoCompromisoPago.PAGADO;
    }

    public static CompromisoPagoBuilder builder() { return new CompromisoPagoBuilder(); }

    public static class CompromisoPagoBuilder {
        private Long id;
        private TipoCompromiso tipoCompromiso;
        private Mes mes;
        private LocalDate fechaEmision;
        private BigDecimal montoOriginal;
        private BigDecimal montoMora = BigDecimal.ZERO;
        private EstadoCompromisoPago estado = EstadoCompromisoPago.PENDIENTE;
        private Matricula matricula;

        public CompromisoPagoBuilder id(Long id) { this.id = id; return this; }
        public CompromisoPagoBuilder tipoCompromiso(TipoCompromiso tipoCompromiso) { this.tipoCompromiso = tipoCompromiso; return this; }
        public CompromisoPagoBuilder mes(Mes mes) { this.mes = mes; return this; }
        public CompromisoPagoBuilder fechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; return this; }
        public CompromisoPagoBuilder montoOriginal(BigDecimal montoOriginal) { this.montoOriginal = montoOriginal; return this; }
        public CompromisoPagoBuilder montoMora(BigDecimal montoMora) { this.montoMora = montoMora; return this; }
        public CompromisoPagoBuilder estado(EstadoCompromisoPago estado) { this.estado = estado; return this; }
        public CompromisoPagoBuilder matricula(Matricula matricula) { this.matricula = matricula; return this; }
        public CompromisoPago build() {
            return new CompromisoPago(id, tipoCompromiso, mes, fechaEmision, montoOriginal, montoMora, estado, matricula);
        }
    }
}