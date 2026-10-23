package com.educoders.Seguridad_Auditoria.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "detalles_auditoria")
public class DetalleAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditoria_id", nullable = false)
    private Auditoria auditoria;

    @Column(name = "campo", nullable = false, length = 120)
    private String campo;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    public DetalleAuditoria() {
    }

    public DetalleAuditoria(String campo, String valorAnterior, String valorNuevo) {
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Auditoria getAuditoria() { return auditoria; }
    public void setAuditoria(Auditoria auditoria) { this.auditoria = auditoria; }

    public String getCampo() { return campo; }
    public void setCampo(String campo) { this.campo = campo; }

    public String getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(String valorAnterior) { this.valorAnterior = valorAnterior; }

    public String getValorNuevo() { return valorNuevo; }
    public void setValorNuevo(String valorNuevo) { this.valorNuevo = valorNuevo; }
}