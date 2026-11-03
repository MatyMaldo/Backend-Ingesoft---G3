package com.educoders.Gestion_Matriz_Horarios.servicio;

public interface HorarioCsvService {
    /** Genera el contenido de la plantilla CSV de horarios (UTF-8 con BOM). */
    byte[] generarPlantilla();
}