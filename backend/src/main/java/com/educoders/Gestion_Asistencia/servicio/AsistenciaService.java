package com.educoders.Gestion_Asistencia.servicio;

import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaDocenteResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaHistorialResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaSesionDetalleResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.GuardarAsistenciaEstudiantesRequest;

import java.time.LocalDate;

public interface AsistenciaService {

    AsistenciaSesionDetalleResponse obtenerDetalle(Long sesionId);

    AsistenciaSesionDetalleResponse guardarAsistenciaEstudiantes(
            Long sesionId,
            GuardarAsistenciaEstudiantesRequest request);

    AsistenciaDocenteResponse registrarAsistenciaDocente(Long sesionId);

    AsistenciaHistorialResponse historialDocente(
            LocalDate desde, LocalDate hasta, Long cursoId, int pagina, int tamano);

    AsistenciaHistorialResponse historialEstudiante(
            LocalDate desde, LocalDate hasta, Long cursoId, int pagina, int tamano);

    AsistenciaHistorialResponse historialApoderado(
            Long estudianteId, LocalDate desde, LocalDate hasta, Long cursoId,
            int pagina, int tamano);
}
