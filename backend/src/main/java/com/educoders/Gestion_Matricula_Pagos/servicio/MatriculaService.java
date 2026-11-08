package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaAdminListResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.SeccionEstudianteResponse;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MatriculaService {

    /**
     * HU-GEMAP-03: Registra la matrícula vinculando al estudiante con la sección elegida.
     */
    MatriculaResponse registrarMatricula(RegistrarMatriculaRequest request);

    /**
     * Devuelve el grado y la sección de un estudiante para un año académico.
     * Considera matrículas PENDIENTE o CONFIRMADA (no CANCELADA).
     */
    SeccionEstudianteResponse obtenerSeccionEstudiante(Long estudianteId, Long anioId);

    /**
     * Listado administrativo de matrículas con filtros opcionales.
     */
    Page<MatriculaAdminListResponse> listarMatriculas(
            Long anioId,
            Long gradoId,
            Long seccionId,
            EstadoMatricula estado,
            Long estudianteId,
            Pageable pageable);
}