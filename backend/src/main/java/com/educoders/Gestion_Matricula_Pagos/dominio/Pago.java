package com.educoders.Gestion_Matricula_Pagos.dominio;

import com.educoders.Infraestructura_Servicios.enums.EstadoPago;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(name = "evidencia_url", length = 500)
    private String evidenciaUrl;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPago estado;

    @Column(name = "monto_pagado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_validador_id")
    private Administrador usuarioValidador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apoderado_pagador_id", nullable = false)
    private Apoderado apoderadoPagador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compromiso_id", nullable = false)
    private CompromisoPago compromisoPago;

    public Pago() {
        this.estado = EstadoPago.PENDIENTE;
    }

    public Pago(Long id, LocalDate fechaRegistro, String evidenciaUrl, String motivo,
                EstadoPago estado, BigDecimal montoPagado, Administrador usuarioValidador,
                Apoderado apoderadoPagador, CompromisoPago compromisoPago) {
        this.id = id;
        this.fechaRegistro = fechaRegistro;
        this.evidenciaUrl = evidenciaUrl;
        this.motivo = motivo;
        this.estado = estado != null ? estado : EstadoPago.PENDIENTE;
        this.montoPagado = montoPagado;
        this.usuarioValidador = usuarioValidador;
        this.apoderadoPagador = apoderadoPagador;
        this.compromisoPago = compromisoPago;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getEvidenciaUrl() { return evidenciaUrl; }
    public void setEvidenciaUrl(String evidenciaUrl) { this.evidenciaUrl = evidenciaUrl; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }

    public BigDecimal getMontoPagado() { return montoPagado; }
    public void setMontoPagado(BigDecimal montoPagado) { this.montoPagado = montoPagado; }

    public Administrador getUsuarioValidador() { return usuarioValidador; }
    public void setUsuarioValidador(Administrador usuarioValidador) { this.usuarioValidador = usuarioValidador; }

    public Apoderado getApoderadoPagador() { return apoderadoPagador; }
    public void setApoderadoPagador(Apoderado apoderadoPagador) { this.apoderadoPagador = apoderadoPagador; }

    public CompromisoPago getCompromisoPago() { return compromisoPago; }
    public void setCompromisoPago(CompromisoPago compromisoPago) { this.compromisoPago = compromisoPago; }

    // Métodos de comportamiento de dominio
    public boolean esPendiente() { return this.estado == EstadoPago.PENDIENTE; }
    public boolean esValidado() { return this.estado == EstadoPago.VALIDADO; }
    public boolean esRechazado() { return this.estado == EstadoPago.RECHAZADO; }

    public static PagoBuilder builder() { return new PagoBuilder(); }

    public static class PagoBuilder {
        private Long id;
        private LocalDate fechaRegistro;
        private String evidenciaUrl;
        private String motivo;
        private EstadoPago estado = EstadoPago.PENDIENTE;
        private BigDecimal montoPagado;
        private Administrador usuarioValidador;
        private Apoderado apoderadoPagador;
        private CompromisoPago compromisoPago;

        public PagoBuilder id(Long id) { this.id = id; return this; }
        public PagoBuilder fechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; return this; }
        public PagoBuilder evidenciaUrl(String evidenciaUrl) { this.evidenciaUrl = evidenciaUrl; return this; }
        public PagoBuilder motivo(String motivo) { this.motivo = motivo; return this; }
        public PagoBuilder estado(EstadoPago estado) { this.estado = estado; return this; }
        public PagoBuilder montoPagado(BigDecimal montoPagado) { this.montoPagado = montoPagado; return this; }
        public PagoBuilder usuarioValidador(Administrador usuarioValidador) { this.usuarioValidador = usuarioValidador; return this; }
        public PagoBuilder apoderadoPagador(Apoderado apoderadoPagador) { this.apoderadoPagador = apoderadoPagador; return this; }
        public PagoBuilder compromisoPago(CompromisoPago compromisoPago) { this.compromisoPago = compromisoPago; return this; }

        public Pago build() {
            return new Pago(id, fechaRegistro, evidenciaUrl, motivo, estado, montoPagado, usuarioValidador, apoderadoPagador, compromisoPago);
        }
    }
}