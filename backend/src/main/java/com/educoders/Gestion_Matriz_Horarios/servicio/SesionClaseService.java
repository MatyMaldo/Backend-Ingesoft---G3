package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GenerarSesionesRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GenerarSesionesResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewSesionesRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewSesionesResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.SesionClaseResponse;

import java.time.LocalDate;
import java.util.List;

public interface SesionClaseService {
    PreviewSesionesResponse previewSesiones(PreviewSesionesRequest request);
    GenerarSesionesResponse generarSesiones(GenerarSesionesRequest request);
    List<SesionClaseResponse> listarPorSeccion(Long seccionId, LocalDate fechaInicio, LocalDate fechaFin);
    SesionClaseResponse obtener(Long id);
}