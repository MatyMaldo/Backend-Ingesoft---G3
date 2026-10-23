package com.educoders.Infraestructura_Servicios.servicio;

import com.cloudinary.Cloudinary;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Primary
public class ServicioAlmacenamientoCloudinaryImpl implements ServicioAlmacenamientoArchivo {

    private final Cloudinary cloudinary;

    // 5 MB expresado en bytes (5 * 1024 * 1024)
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    public ServicioAlmacenamientoCloudinaryImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String guardarArchivo(MultipartFile archivo, String directorioDestino) {
        // 1. Validar que sea obligatorio
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("El comprobante es obligatorio.");
        }

        // 2. Validar tamaño máximo (5 MB)
        if (archivo.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ReglaNegocioException("El comprobante supera el tamaño máximo permitido de 5 MB.");
        }

        // 3. Validar formato estrictamente PDF
        String contentType = archivo.getContentType();
        String nombreOriginal = archivo.getOriginalFilename();

        boolean esContentTypePdf = contentType != null && contentType.equalsIgnoreCase("application/pdf");
        boolean esExtensionPdf = nombreOriginal != null && nombreOriginal.toLowerCase().endsWith(".pdf");

        if (!esContentTypePdf && !esExtensionPdf) {
            throw new ReglaNegocioException("Solo se acepta el comprobante en formato PDF.");
        }

        try {
            Map<String, Object> opciones = Map.of(
                    "folder", directorioDestino,
                    "resource_type", "auto",
                    "overwrite", true
            );

            Map<?, ?> resultado = cloudinary.uploader().upload(archivo.getBytes(), opciones);
            return (String) resultado.get("secure_url");

        } catch (IOException e) {
            throw new ReglaNegocioException("Error al subir el comprobante PDF a Cloudinary: " + e.getMessage());
        }
    }

    /*
    No se deberia borrar ya que quedan como evidencias de pago(o de pagos/archivos incorrectos)
    @Override
    public void eliminarArchivo(String rutaUrl) {
        // Reservado para futuras eliminaciones
    }
    */
}