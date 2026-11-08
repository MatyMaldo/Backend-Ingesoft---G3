package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;

import java.time.LocalDate;

/**
 * Fila del listado administrativo de matrículas.
 */
public record MatriculaAdminListResponse(
        Long id,
        LocalDate fechaAsignacion,
        EstadoMatricula estado,
        Long estudianteId,
        String nombreCompletoEstudiante,
        String codigoEstudiante,
        Long gradoId,
        String gradoNombre,
        Long seccionId,
        String seccionNombre,
        Long anioAcademicoId,
        Integer anioAcademico
) {

    public static MatriculaAdminListResponse fromEntity(Matricula matricula) {
        var estudiante = matricula.getEstudiante();
        var seccion = matricula.getSeccion();
        var grado = seccion.getGrado();
        var anio = seccion.getAnioAcademico();

        String nombreCompleto = estudiante.getNombre() + " " + estudiante.getApellidoPaterno();
        if (estudiante.getApellidoMaterno() != null && !estudiante.getApellidoMaterno().isBlank()) {
            nombreCompleto += " " + estudiante.getApellidoMaterno();
        }

        return new MatriculaAdminListResponse(
                matricula.getId(),
                matricula.getFechaAsignacion(),
                matricula.getEstado(),
                estudiante.getId(),
                nombreCompleto,
                estudiante.getCodigo(),
                grado.getId(),
                grado.getNombre(),
                seccion.getId(),
                seccion.getNombre(),
                anio.getId(),
                anio.getAnio()
        );
    }
}
