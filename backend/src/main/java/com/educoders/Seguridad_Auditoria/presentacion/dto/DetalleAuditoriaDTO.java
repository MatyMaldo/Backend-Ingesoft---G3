package com.educoders.Seguridad_Auditoria.presentacion.dto;

public class DetalleAuditoriaDTO {
    private String campo;
    private String valorAnterior;
    private String valorNuevo;

    public DetalleAuditoriaDTO() {
    }

    public DetalleAuditoriaDTO(String campo, String valorAnterior, String valorNuevo) {
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
    }

    public String getCampo() { return campo; }
    public void setCampo(String campo) { this.campo = campo; }

    public String getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(String valorAnterior) { this.valorAnterior = valorAnterior; }

    public String getValorNuevo() { return valorNuevo; }
    public void setValorNuevo(String valorNuevo) { this.valorNuevo = valorNuevo; }
}