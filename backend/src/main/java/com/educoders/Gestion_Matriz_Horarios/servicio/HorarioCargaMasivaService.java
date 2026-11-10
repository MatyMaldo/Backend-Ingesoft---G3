package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ConfirmarCargaMasivaRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ConfirmarCargaMasivaResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ExportarBorradorRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewCargaMasivaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HorarioCargaMasivaService {

    PreviewCargaMasivaResponse preview(List<MultipartFile> archivos, Long anioAcademicoId);

    ConfirmarCargaMasivaResponse confirmar(ConfirmarCargaMasivaRequest request);

    byte[] exportar(Long anioId, Long gradoId, Long seccionId, Long docenteId, String formato);

    /**
     * Exporta una lista de bloques en memoria sin persistirlos.
     */
    byte[] exportarBorrador(ExportarBorradorRequest request);

    byte[] generarPlantilla(String formato);
}
