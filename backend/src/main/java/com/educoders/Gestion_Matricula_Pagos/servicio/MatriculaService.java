package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EstudianteAsignacionResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaAdminListResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculasPorEstadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.SeccionEstudianteResponse;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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
     *
     * @param busqueda texto libre que se busca en el nombre completo o el código del estudiante
     *                 (sin distinguir mayúsculas); {@code null} o en blanco no filtra
     */
    Page<MatriculaAdminListResponse> listarMatriculas(
            Long anioId,
            Long gradoId,
            Long seccionId,
            EstadoMatricula estado,
            Long estudianteId,
            String busqueda,
            Pageable pageable);

    /**
     * Cuenta las matrículas por estado con los filtros del listado (año, grado, sección y búsqueda; no el estado).
     * Alimenta los contadores de los chips de estado.
     */
    MatriculasPorEstadoResponse contarMatriculasPorEstado(
            Long anioId,
            Long gradoId,
            Long seccionId,
            String busqueda);

    /**
     * Cancela matrículas pendientes cuyo periodo de matrícula ya terminó y
     * que no tienen un compromiso de matrícula pagado.
     *
     * @return cantidad de matrículas canceladas
     */
    int cancelarPendientesPorPeriodoVencido();

    /**
     * Lista estudiantes activos con matrícula confirmada para una sección específica.
     */
    List<EstudianteAsignacionResponse> listarEstudiantesActivosPorSeccion(Long seccionId);
}