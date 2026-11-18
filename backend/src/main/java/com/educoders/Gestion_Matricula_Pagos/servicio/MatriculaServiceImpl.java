package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EstudianteAsignacionResponse;
import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.CompromisoPagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ActualizarSeccionMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaAdminListResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculasPorEstadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.SeccionEstudianteResponse;
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MatriculaServiceImpl implements MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final PeriodoMatriculaRepository periodoMatriculaRepository;
    private final SeccionRepository seccionRepository;
    private final EstudianteRepository estudianteRepository;
    private final CompromisoPagoRepository compromisoPagoRepository;
    private final PagoRepository pagoRepository;
    private final AuditoriaService auditoriaService;

    public MatriculaServiceImpl(MatriculaRepository matriculaRepository,
                                PeriodoMatriculaRepository periodoMatriculaRepository,
                                SeccionRepository seccionRepository,
                                EstudianteRepository estudianteRepository,
                                CompromisoPagoRepository compromisoPagoRepository,
                                PagoRepository pagoRepository,
                                AuditoriaService auditoriaService) {
        this.matriculaRepository = matriculaRepository;
        this.periodoMatriculaRepository = periodoMatriculaRepository;
        this.seccionRepository = seccionRepository;
        this.estudianteRepository = estudianteRepository;
        this.compromisoPagoRepository = compromisoPagoRepository;
        this.pagoRepository = pagoRepository;
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
            String busqueda,
            Pageable pageable) {

        exigirAdministrador();

        String patron = busqueda == null || busqueda.isBlank()
                ? ""
                : "%" + busqueda.trim().toLowerCase() + "%";

        Page<Matricula> pagina = matriculaRepository.listarMatriculas(
                anioId, gradoId, seccionId, estado, estudianteId, patron, pageable);

        List<MatriculaAdminListResponse> contenido = enriquecer(pagina.getContent());
        return new PageImpl<>(contenido, pageable, pagina.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public MatriculasPorEstadoResponse contarMatriculasPorEstado(
            Long anioId,
            Long gradoId,
            Long seccionId,
            String busqueda) {

        exigirAdministrador();

        String patron = busqueda == null || busqueda.isBlank()
                ? ""
                : "%" + busqueda.trim().toLowerCase() + "%";

        long pendientes = 0;
        long confirmadas = 0;
        long canceladas = 0;
        for (Object[] fila : matriculaRepository.contarPorEstado(anioId, gradoId, seccionId, patron)) {
            long cantidad = ((Number) fila[1]).longValue();
            switch ((EstadoMatricula) fila[0]) {
                case PENDIENTE -> pendientes = cantidad;
                case CONFIRMADA -> confirmadas = cantidad;
                case CANCELADA -> canceladas = cantidad;
            }
        }
        return new MatriculasPorEstadoResponse(pendientes + confirmadas + canceladas, pendientes, confirmadas, canceladas);
    }

    @Override
    @Transactional
    public int cancelarPendientesPorPeriodoVencido() {
        LocalDate hoy = LocalDate.now();
        int canceladas = 0;

        for (PeriodoMatricula periodo : periodoMatriculaRepository.findAll()) {
            if (periodo.getFechaFin() == null || !hoy.isAfter(periodo.getFechaFin())
                    || periodo.getAnioAcademico() == null) {
                continue;
            }

            List<Matricula> pendientes = matriculaRepository.buscarSinCompromisoPagado(
                    periodo.getAnioAcademico().getId(),
                    EstadoMatricula.PENDIENTE,
                    TipoCompromiso.MATRICULA,
                    EstadoCompromisoPago.PAGADO
            );

            if (pendientes.isEmpty()) {
                continue;
            }

            for (Matricula matricula : pendientes) {
                matricula.setEstado(EstadoMatricula.CANCELADA);
                auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                        null,
                        CategoriaAuditoria.MATRICULA,
                        TipoAccionAuditoria.CAMBIO_ESTADO,
                        "Matricula",
                        matricula.getId(),
                        "Cancelación automática: periodo de matrícula vencido sin compromiso de matrícula pagado",
                        List.of(new DetalleAuditoriaDTO(
                                "estado",
                                EstadoMatricula.PENDIENTE.name(),
                                EstadoMatricula.CANCELADA.name()))
                ));
            }

            matriculaRepository.saveAll(pendientes);
            canceladas += pendientes.size();
        }

        return canceladas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstudianteAsignacionResponse> listarEstudiantesActivosPorSeccion(Long seccionId) {
        List<Matricula> matriculas = matriculaRepository.buscarPorSeccionYEstadoConEstudiante(
                seccionId,
                EstadoMatricula.CONFIRMADA
        );

        return matriculas.stream()
                .map(m -> new EstudianteAsignacionResponse(
                        m.getEstudiante().getId(),
                        m.getEstudiante().getCodigo(),
                        m.getEstudiante().getNombre(),
                        m.getEstudiante().getApellidoPaterno(),
                        m.getEstudiante().getApellidoMaterno(),
                        m.getEstado()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MatriculaAdminListResponse actualizarSeccion(
            Long id, ActualizarSeccionMatriculaRequest request) {

        exigirAdministrador();

        if (request == null || request.seccionId() == null) {
            throw new ReglaNegocioException("La sección es obligatoria.");
        }

        Matricula matricula = matriculaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Matrícula", id));

        if (matricula.getEstado() != EstadoMatricula.PENDIENTE) {
            throw new ConflictoException("Solo se pueden editar matrículas PENDIENTE.");
        }

        Seccion anterior = matricula.getSeccion();
        AnioAcademico anio = anterior.getAnioAcademico();
        exigirPeriodoVigente(anio);

        if (anterior.getId().equals(request.seccionId())) {
            return enriquecer(List.of(matricula)).get(0);
        }

        Seccion nueva = seccionRepository.findById(request.seccionId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Sección", request.seccionId()));

        if (nueva.getAnioAcademico() == null
                || !nueva.getAnioAcademico().getId().equals(anio.getId())) {
            throw new ReglaNegocioException(
                    "La sección debe pertenecer al mismo año académico de la matrícula.");
        }

        Long estudianteId = matricula.getEstudiante().getId();

        boolean duplicadoEnAnio = matriculaRepository
                .existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoInAndIdNot(
                        estudianteId,
                        anio.getId(),
                        List.of(EstadoMatricula.PENDIENTE, EstadoMatricula.CONFIRMADA),
                        matricula.getId());

        if (duplicadoEnAnio) {
            throw new ConflictoException(
                    "El estudiante ya cuenta con otra matrícula vigente en este año académico.");
        }

        boolean yaExisteEnSeccion = matriculaRepository
                .existsByEstudianteIdAndSeccionIdAndIdNot(
                        estudianteId, nueva.getId(), matricula.getId());

        if (yaExisteEnSeccion) {
            throw new ConflictoException(
                    "El estudiante ya tiene un registro de matrícula en la sección destino, "
                            + "aunque esté cancelada. Elige otra sección.");
        }

        long ocupacion = matriculaRepository.countBySeccionIdAndEstadoInAndIdNot(
                nueva.getId(),
                List.of(EstadoMatricula.PENDIENTE, EstadoMatricula.CONFIRMADA),
                matricula.getId());

        if (ocupacion >= nueva.getCapacidadMaxima()) {
            throw new ReglaNegocioException(
                    "La sección seleccionada ha alcanzado su capacidad máxima de estudiantes ("
                            + nueva.getCapacidadMaxima() + ").");
        }

        matricula.setSeccion(nueva);
        Matricula guardada;
        try {
            guardada = matriculaRepository.save(matricula);
            matriculaRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictoException(
                    "No se pudo cambiar la sección: ya existe otro registro de matrícula "
                            + "del estudiante en la sección destino.");
        }

        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                null,
                CategoriaAuditoria.MATRICULA,
                TipoAccionAuditoria.ACTUALIZAR,
                "Matricula",
                guardada.getId(),
                "Cambio de sección de matrícula",
                List.of(
                        new DetalleAuditoriaDTO(
                                "seccionId",
                                String.valueOf(anterior.getId()),
                                String.valueOf(nueva.getId())),
                        new DetalleAuditoriaDTO(
                                "seccion",
                                anterior.getNombre(),
                                nueva.getNombre()),
                        new DetalleAuditoriaDTO(
                                "grado",
                                anterior.getGrado().getNombre(),
                                nueva.getGrado().getNombre())
                )
        ));

        return enriquecer(List.of(guardada)).get(0);
    }

    @Override
    @Transactional
    public void eliminarMatricula(Long id) {
        exigirAdministrador();

        Matricula matricula = matriculaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Matrícula", id));

        if (matricula.getEstado() != EstadoMatricula.PENDIENTE) {
            throw new ConflictoException("Solo se pueden eliminar matrículas PENDIENTE.");
        }

        AnioAcademico anio = matricula.getSeccion().getAnioAcademico();
        exigirPeriodoVigente(anio);

        List<CompromisoPago> compromisos = compromisoPagoRepository.findByMatriculaId(id);

        if (compromisos.size() > 1) {
            throw new ConflictoException(
                    "La matrícula tiene varios compromisos asociados; no se puede eliminar automáticamente.");
        }

        CompromisoPago compromiso = compromisos.isEmpty() ? null : compromisos.get(0);

        if (compromiso != null) {
            if (compromiso.getTipoCompromiso() == TipoCompromiso.PENSION) {
                throw new ConflictoException(
                        "La matrícula tiene un compromiso de pensión asociado; no se puede eliminar.");
            }
            if (compromiso.getEstado() == EstadoCompromisoPago.PAGADO) {
                throw new ConflictoException(
                        "El compromiso de matrícula está marcado como pagado; no se puede eliminar.");
            }
            if (!pagoRepository.findByCompromisoPagoId(compromiso.getId()).isEmpty()) {
                throw new ConflictoException(
                        "La matrícula tiene pagos registrados; no se puede eliminar.");
            }
        }

        List<DetalleAuditoriaDTO> detalles = new ArrayList<>();
        detalles.add(new DetalleAuditoriaDTO(
                "estudianteId", String.valueOf(matricula.getEstudiante().getId()), null));
        detalles.add(new DetalleAuditoriaDTO(
                "seccionId", String.valueOf(matricula.getSeccion().getId()), null));
        detalles.add(new DetalleAuditoriaDTO(
                "anioAcademicoId", String.valueOf(anio.getId()), null));
        detalles.add(new DetalleAuditoriaDTO(
                "estado", matricula.getEstado().name(), null));
        if (compromiso != null) {
            detalles.add(new DetalleAuditoriaDTO(
                    "compromisoId", String.valueOf(compromiso.getId()), null));
        }

        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                null,
                CategoriaAuditoria.MATRICULA,
                TipoAccionAuditoria.ELIMINAR,
                "Matricula",
                matricula.getId(),
                "Eliminación de matrícula PENDIENTE",
                detalles
        ));

        try {
            if (compromiso != null) {
                compromisoPagoRepository.delete(compromiso);
                compromisoPagoRepository.flush();
            }
            matriculaRepository.delete(matricula);
            matriculaRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictoException(
                    "No se pudo eliminar la matrícula porque tiene registros dependientes.");
        }
    }

    private void exigirPeriodoVigente(AnioAcademico anio) {
        PeriodoMatricula periodo = periodoMatriculaRepository
                .findByAnioAcademicoId(anio.getId())
                .orElseThrow(() -> new ReglaNegocioException(
                        "El período de matrícula no está habilitado para el año académico "
                                + anio.getAnio() + "."));

        if (!periodo.estaVigente(LocalDate.now())) {
            throw new ReglaNegocioException(
                    "El período de matrícula ha finalizado o aún no se encuentra vigente.");
        }
    }

    /**
     * Agrega los permisos y motivos del listado consultando en lote los periodos,
     * compromisos y pagos de la página. Evita una consulta por fila.
     */
    private List<MatriculaAdminListResponse> enriquecer(List<Matricula> matriculas) {
        if (matriculas.isEmpty()) {
            return List.of();
        }

        LocalDate hoy = LocalDate.now();

        Map<Long, PeriodoMatricula> periodosPorAnio = periodoMatriculaRepository.findAll().stream()
                .filter(p -> p.getAnioAcademico() != null)
                .collect(Collectors.toMap(
                        p -> p.getAnioAcademico().getId(),
                        p -> p,
                        (a, b) -> a));

        List<Long> ids = matriculas.stream().map(Matricula::getId).toList();
        List<CompromisoPago> compromisos = compromisoPagoRepository.findByMatriculaIdIn(ids);

        Map<Long, List<CompromisoPago>> compromisosPorMatricula = compromisos.stream()
                .collect(Collectors.groupingBy(c -> c.getMatricula().getId()));

        Map<Long, Long> pagosPorCompromiso = new HashMap<>();
        List<Long> compromisoIds = compromisos.stream().map(CompromisoPago::getId).toList();
        if (!compromisoIds.isEmpty()) {
            pagoRepository.findByCompromisoPagoIdIn(compromisoIds).forEach(p ->
                    pagosPorCompromiso.merge(p.getCompromisoPago().getId(), 1L, Long::sum));
        }

        List<MatriculaAdminListResponse> resultado = new ArrayList<>(matriculas.size());
        for (Matricula matricula : matriculas) {
            resultado.add(construirAdminResponse(
                    matricula, hoy, periodosPorAnio, compromisosPorMatricula, pagosPorCompromiso));
        }
        return resultado;
    }

    private MatriculaAdminListResponse construirAdminResponse(
            Matricula matricula,
            LocalDate hoy,
            Map<Long, PeriodoMatricula> periodosPorAnio,
            Map<Long, List<CompromisoPago>> compromisosPorMatricula,
            Map<Long, Long> pagosPorCompromiso) {

        boolean pendiente = matricula.getEstado() == EstadoMatricula.PENDIENTE;
        Long anioId = matricula.getSeccion().getAnioAcademico().getId();
        PeriodoMatricula periodo = periodosPorAnio.get(anioId);
        boolean vigente = periodo != null && periodo.estaVigente(hoy);

        String motivoNoEditar = null;
        String motivoNoEliminar = null;
        boolean puedeEditar = false;
        boolean puedeEliminar = false;

        if (!pendiente) {
            motivoNoEditar = "Solo se pueden editar matrículas PENDIENTE.";
            motivoNoEliminar = "Solo se pueden eliminar matrículas PENDIENTE.";
        } else if (!vigente) {
            motivoNoEditar = "El periodo de matrícula no está vigente.";
            motivoNoEliminar = "El periodo de matrícula no está vigente.";
        } else {
            puedeEditar = true;

            List<CompromisoPago> compromisos = compromisosPorMatricula
                    .getOrDefault(matricula.getId(), List.of());

            if (compromisos.size() > 1) {
                motivoNoEliminar = "La matrícula tiene varios compromisos asociados.";
            } else if (compromisos.size() == 1) {
                CompromisoPago compromiso = compromisos.get(0);
                if (compromiso.getTipoCompromiso() == TipoCompromiso.PENSION) {
                    motivoNoEliminar = "La matrícula tiene un compromiso de pensión asociado.";
                } else if (compromiso.getEstado() == EstadoCompromisoPago.PAGADO) {
                    motivoNoEliminar = "El compromiso de matrícula está marcado como pagado.";
                } else if (pagosPorCompromiso.getOrDefault(compromiso.getId(), 0L) > 0) {
                    motivoNoEliminar = "La matrícula tiene pagos registrados.";
                }
            }

            puedeEliminar = motivoNoEliminar == null;
        }

        return MatriculaAdminListResponse.fromEntity(
                matricula, puedeEditar, puedeEliminar, motivoNoEditar, motivoNoEliminar);
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