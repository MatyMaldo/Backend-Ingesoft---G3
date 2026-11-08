package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaAdminListResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.SeccionEstudianteResponse;
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleAuditoriaDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class MatriculaServiceImpl implements MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final PeriodoMatriculaRepository periodoMatriculaRepository;
    private final SeccionRepository seccionRepository;
    private final EstudianteRepository estudianteRepository;
    private final AuditoriaService auditoriaService;

    public MatriculaServiceImpl(MatriculaRepository matriculaRepository,
                                PeriodoMatriculaRepository periodoMatriculaRepository,
                                SeccionRepository seccionRepository,
                                EstudianteRepository estudianteRepository,
                                AuditoriaService auditoriaService) {
        this.matriculaRepository = matriculaRepository;
        this.periodoMatriculaRepository = periodoMatriculaRepository;
        this.seccionRepository = seccionRepository;
        this.estudianteRepository = estudianteRepository;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public MatriculaResponse registrarMatricula(RegistrarMatriculaRequest request) {
        exigirAdministrador();
        LocalDate hoy = LocalDate.now();

        // 1. Búsqueda y verificación de la Sección
        Seccion seccion = seccionRepository.findById(request.seccionId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Sección", request.seccionId()));

        AnioAcademico anioAcademico = seccion.getAnioAcademico();

        // 2. Verificación del Período de Matrícula Vigente
        PeriodoMatricula periodo = periodoMatriculaRepository.findByAnioAcademicoId(anioAcademico.getId())
                .orElseThrow(() -> new ReglaNegocioException(
                        "El período de matrícula no está habilitado para el año académico " + anioAcademico.getAnio() + "."));

        if (!periodo.estaVigente(hoy)) {
            throw new ReglaNegocioException("El período de matrícula ha finalizado o aún no se encuentra vigente.");
        }

        // 3. Búsqueda y verificación del Estudiante
        Estudiante estudiante = estudianteRepository.findById(request.estudianteId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Estudiante", request.estudianteId()));

        if (!estudiante.isActivo()) {
            throw new ReglaNegocioException("No se puede matricular a un estudiante inactivo.");
        }

        // 4. Validación de Matrícula Duplicada en el Año Académico
        boolean yaRegistrado = matriculaRepository.existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoIn(
                estudiante.getId(),
                anioAcademico.getId(),
                List.of(EstadoMatricula.PENDIENTE, EstadoMatricula.CONFIRMADA)
        );

        if (yaRegistrado) {
            throw new ConflictoException("El estudiante ya cuenta con una matrícula registrada para este año académico.");
        }

        // 5. Validación de Capacidad Máxima de la Sección
        long inscritosActuales = matriculaRepository.countBySeccionIdAndEstadoIn(
                seccion.getId(),
                List.of(EstadoMatricula.PENDIENTE, EstadoMatricula.CONFIRMADA)
        );

        if (inscritosActuales >= seccion.getCapacidadMaxima()) {
            throw new ReglaNegocioException(
                    "La sección seleccionada ha alcanzado su capacidad máxima de estudiantes (" + seccion.getCapacidadMaxima() + ").");
        }

        // 6. Construcción y Persistencia
        Matricula matricula = Matricula.builder()
                .fechaAsignacion(hoy)
                .estado(EstadoMatricula.PENDIENTE)
                .estudiante(estudiante)
                .seccion(seccion)
                .build();

        Matricula guardada = matriculaRepository.save(matricula);

        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                null,
                CategoriaAuditoria.MATRICULA,
                TipoAccionAuditoria.CREAR,
                "Matricula",
                guardada.getId(),
                "Registro de matrícula",
                List.of(
                        new DetalleAuditoriaDTO("estudianteId", null, String.valueOf(estudiante.getId())),
                        new DetalleAuditoriaDTO("seccionId", null, String.valueOf(seccion.getId())),
                        new DetalleAuditoriaDTO("estado", null, guardada.getEstado().name())
                )
        ));

        return mapToResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public SeccionEstudianteResponse obtenerSeccionEstudiante(Long estudianteId, Long anioId) {
        obtenerUsuarioAutenticado();

        if (estudianteId == null || anioId == null) {
            throw new ReglaNegocioException(
                    "El estudiante y el año académico son obligatorios.");
        }

        Matricula matricula = matriculaRepository
                .buscarPorEstudianteYAnioIdYEstados(
                        estudianteId,
                        anioId,
                        List.of(EstadoMatricula.PENDIENTE, EstadoMatricula.CONFIRMADA))
                .stream()
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró una matrícula vigente para el estudiante " + estudianteId
                                + " en el año académico indicado."));

        return new SeccionEstudianteResponse(
                matricula.getId(),
                matricula.getEstudiante().getId(),
                matricula.getSeccion().getAnioAcademico().getId(),
                matricula.getSeccion().getAnioAcademico().getAnio(),
                matricula.getEstado(),
                matricula.getSeccion().getGrado().getId(),
                matricula.getSeccion().getGrado().getNombre(),
                matricula.getSeccion().getId(),
                matricula.getSeccion().getNombre()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatriculaAdminListResponse> listarMatriculas(
            Long anioId,
            Long gradoId,
            Long seccionId,
            EstadoMatricula estado,
            Long estudianteId,
            Pageable pageable) {

        exigirAdministrador();

        return matriculaRepository.listarMatriculas(
                        anioId, gradoId, seccionId, estado, estudianteId, pageable)
                .map(MatriculaAdminListResponse::fromEntity);
    }

    private void exigirAdministrador() {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (!(usuario instanceof Administrador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un administrador puede registrar matrículas.");
        }
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return usuario;
    }

    private MatriculaResponse mapToResponse(Matricula m) {
        String nombreCompleto = m.getEstudiante().getNombre() + " " + m.getEstudiante().getApellidoPaterno();
        if (m.getEstudiante().getApellidoMaterno() != null) {
            nombreCompleto += " " + m.getEstudiante().getApellidoMaterno();
        }

        return new MatriculaResponse(
                m.getId(),
                m.getFechaAsignacion(),
                m.getEstado(),
                m.getEstudiante().getId(),
                nombreCompleto,
                m.getEstudiante().getCodigo(),
                m.getSeccion().getId(),
                m.getSeccion().getNombre(),
                m.getSeccion().getGrado().getNombre(),
                m.getSeccion().getAnioAcademico().getAnio()
        );
    }
}