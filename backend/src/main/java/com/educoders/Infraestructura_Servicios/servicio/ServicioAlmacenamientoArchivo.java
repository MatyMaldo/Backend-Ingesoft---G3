package com.educoders.Infraestructura_Servicios.servicio;

import org.springframework.web.multipart.MultipartFile;

public interface ServicioAlmacenamientoArchivo {

    /**
     * Sube un archivo (PDF de comprobante o imagen) al servidor o bucket
     * y retorna la URL pública/relativa para persistir en BD.
     */
    String guardarArchivo(MultipartFile archivo, String directorioDestino);

    /**
     * Elimina un archivo mediante su URL almacenada si la operación es rechazada/revertida.
     */
    // void eliminarArchivo(String rutaUrl);
}