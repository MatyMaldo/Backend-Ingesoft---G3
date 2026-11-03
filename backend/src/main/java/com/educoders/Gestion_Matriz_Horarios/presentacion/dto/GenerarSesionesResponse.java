package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import java.util.ArrayList;
import java.util.List;

public class GenerarSesionesResponse {

    private int totalBloquesProcesados;
    private int sesionesCreadas;
    private int sesionesYaExistentes;
    private int sesionesOmitidas;
    private List<String> errores = new ArrayList<>();

    public GenerarSesionesResponse() {
    }

    public int getTotalBloquesProcesados() { return totalBloquesProcesados; }
    public void setTotalBloquesProcesados(int totalBloquesProcesados) { this.totalBloquesProcesados = totalBloquesProcesados; }
    public int getSesionesCreadas() { return sesionesCreadas; }
    public void setSesionesCreadas(int sesionesCreadas) { this.sesionesCreadas = sesionesCreadas; }
    public int getSesionesYaExistentes() { return sesionesYaExistentes; }
    public void setSesionesYaExistentes(int sesionesYaExistentes) { this.sesionesYaExistentes = sesionesYaExistentes; }
    public int getSesionesOmitidas() { return sesionesOmitidas; }
    public void setSesionesOmitidas(int sesionesOmitidas) { this.sesionesOmitidas = sesionesOmitidas; }
    public List<String> getErrores() { return errores; }
    public void setErrores(List<String> errores) { this.errores = errores; }
}