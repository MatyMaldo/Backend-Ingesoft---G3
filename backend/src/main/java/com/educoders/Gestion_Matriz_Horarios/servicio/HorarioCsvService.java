package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.CargaMasivaResponse;
import org.springframework.web.multipart.MultipartFile;

public interface HorarioCsvService {
    /** Genera el contenido de la plantilla CSV de horarios (UTF-8 con BOM). */
    byte[] generarPlantilla();

    /** Carga masiva de bloques horarios. Todo o nada; devuelve la lista completa de errores. */
    CargaMasivaResponse cargarMasivo(MultipartFile archivo, Long anioAcademicoId);
}