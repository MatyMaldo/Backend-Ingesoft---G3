package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import java.util.ArrayList;
import java.util.List;

public class CargaMasivaResponse {

    private int filasProcesadas;
    private int bloquesCreados;
    private List<ErrorFila> errores = new ArrayList<>();

    public CargaMasivaResponse() {
    }

    public int getFilasProcesadas() { return filasProcesadas; }
    public void setFilasProcesadas(int filasProcesadas) { this.filasProcesadas = filasProcesadas; }
    public int getBloquesCreados() { return bloquesCreados; }
    public void setBloquesCreados(int bloquesCreados) { this.bloquesCreados = bloquesCreados; }
    public List<ErrorFila> getErrores() { return errores; }
    public void setErrores(List<ErrorFila> errores) { this.errores = errores; }
}