package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

public class ErrorFila {

    private int fila;
    private String mensaje;

    public ErrorFila() {
    }

    public ErrorFila(int fila, String mensaje) {
        this.fila = fila;
        this.mensaje = mensaje;
    }

    public int getFila() { return fila; }
    public void setFila(int fila) { this.fila = fila; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}