package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.SesionClaseResponse;
import java.time.LocalDate;
import java.util.List;

public interface SesionClaseService {
    List<SesionClaseResponse> listarPorSeccion(Long seccionId, LocalDate fechaInicio, LocalDate fechaFin);
    SesionClaseResponse obtener(Long id);
}