package com.educoders.Gestion_Academica.servicio;

import com.educoders.Gestion_Academica.presentacion.dto.*;
import java.util.List;

public interface GradoSeccionService {
    GradoResponse crearGrado(CrearGradoRequest request);
    List<GradoResponse> listarGrados();
    GradoResponse obtenerGradoPorId(Long id);

    SeccionResponse crearSeccion(CrearSeccionRequest request);
    List<SeccionResponse> listarSecciones();
    List<SeccionResponse> listarSeccionesPorGradoYAnio(Long gradoId, Long anioId);
}