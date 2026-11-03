package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import java.util.ArrayList;
import java.util.List;

public class PreviewSesionesResponse {

    private List<SesionPreviewItem> sesiones = new ArrayList<>();
    private int total;

    public PreviewSesionesResponse() {
    }

    public List<SesionPreviewItem> getSesiones() { return sesiones; }
    public void setSesiones(List<SesionPreviewItem> sesiones) { this.sesiones = sesiones; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}