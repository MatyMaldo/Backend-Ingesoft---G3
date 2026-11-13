package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GenerarSesionesRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GenerarSesionesResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewSesionesRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewSesionesResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.SesionClaseResponse;
import com.educoders.Infraestructura_Servicios.operaciones.ProgresoLote;

import java.time.LocalDate;
import java.util.List;

public interface SesionClaseService {
    PreviewSesionesResponse previewSesiones(PreviewSesionesRequest request);
    GenerarSesionesResponse generarSesiones(GenerarSesionesRequest request);

    /**
     * Variante que reporta el avance por día del rango solicitado.
     */
    GenerarSesionesResponse generarSesiones(GenerarSesionesRequest request,
                                            ProgresoLote progreso);

    /**
     * Omite o restaura una sesión programada. Solo se permite antes de que
     * empiece; las sesiones activas o finalizadas no se modifican.
     */
    SesionClaseResponse cambiarOmision(Long id, boolean omitida);

    List<SesionClaseResponse> listarPorSeccion(Long seccionId, LocalDate fechaInicio, LocalDate fechaFin);
    List<SesionClaseResponse> listarPorDocente(Long docenteId, LocalDate fechaInicio, LocalDate fechaFin);
    List<SesionClaseResponse> listarMiHorarioDocente(LocalDate fechaInicio, LocalDate fechaFin);
    List<SesionClaseResponse> listarMiHorarioEstudiante(LocalDate fechaInicio, LocalDate fechaFin);
    List<SesionClaseResponse> listarPorApoderado(Long estudianteId, LocalDate fechaInicio, LocalDate fechaFin);
    SesionClaseResponse obtener(Long id);
}