package com.educoders.Gestion_Matriz_Horarios.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de la jornada escolar y del receso.
 * Los valores se pueden sobreescribir en application.yml/properties.
 */
@Component
@ConfigurationProperties(prefix = "app.horarios")
public class HorarioProperties {

    private String jornadaInicio = "08:00";
    private String jornadaFin = "13:30";
    private String recesoInicio = "11:00";
    private String recesoFin = "11:30";

    public String getJornadaInicio() {
        return jornadaInicio;
    }

    public void setJornadaInicio(String jornadaInicio) {
        this.jornadaInicio = jornadaInicio;
    }

    public String getJornadaFin() {
        return jornadaFin;
    }

    public void setJornadaFin(String jornadaFin) {
        this.jornadaFin = jornadaFin;
    }

    public String getRecesoInicio() {
        return recesoInicio;
    }

    public void setRecesoInicio(String recesoInicio) {
        this.recesoInicio = recesoInicio;
    }

    public String getRecesoFin() {
        return recesoFin;
    }

    public void setRecesoFin(String recesoFin) {
        this.recesoFin = recesoFin;
    }
}
