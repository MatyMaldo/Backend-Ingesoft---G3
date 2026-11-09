package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.Pago;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.CompromisoPagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.*;
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoPago;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.servicio.ServicioAlmacenamientoArchivo;
import com.educoders.Infraestructura_Servicios.servicio.ServicioNotificacionEmail;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final CompromisoPagoRepository compromisoPagoRepository;
    private final PeriodoMatriculaRepository periodoMatriculaRepository;
    private final ServicioAlmacenamientoArchivo servicioAlmacenamientoArchivo;
    private final ServicioNotificacionEmail servicioNotificacionEmail;
    private final AuditoriaService auditoriaService;

    public PagoServiceImpl(PagoRepository pagoRepository,
                           CompromisoPagoRepository compromisoPagoRepository,
                           PeriodoMatriculaRepository periodoMatriculaRepository,
                           ServicioAlmacenamientoArchivo servicioAlmacenamientoArchivo,
                           ServicioNotificacionEmail servicioNotificacionEmail,
                           AuditoriaService auditoriaService) {
        this.pagoRepository = pagoRepository;
        this.compromisoPagoRepository = compromisoPagoRepository;
        this.periodoMatriculaRepository = periodoMatriculaRepository;
        this.servicioAlmacenamientoArchivo = servicioAlmacenamientoArchivo;
        this.servicioNotificacionEmail = servicioNotificacionEmail;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoApoderadoResponse> listarPagosPorCompromisoYApoderado(Long compromisoId) {
        Apoderado apoderado = exigirApoderado();

        CompromisoPago compromiso = compromisoPagoRepository.findById(compromisoId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Compromiso de Pago", compromisoId));

        Long apoderadoAsociadoId = compromiso.getMatricula().getEstudiante().getApoderado().getId();
        if (!apoderadoAsociadoId.equals(apoderado.getId())) {
            throw new ReglaNegocioException("El compromiso de pago no pertenece a ningún estudiante a su cargo.");
        }

        return pagoRepository.findByCompromisoPagoId(compromisoId)
                .stream()
                .map(this::mapToPagoApoderadoResponse)
                .toList();
    }

    @Override
    @Transactional
    public PagoApoderadoResponse registrarPagoApoderado(RegistrarPagoRequest request) {
        Apoderado apoderado = exigirApoderado();

        // 1. Validar existencia del compromiso
        CompromisoPago compromiso = compromisoPagoRepository.findById(request.compromisoId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Compromiso de Pago", request.compromisoId()));

        // 2. Validar que el compromiso pertenezca a un estudiante a cargo del apoderado autenticado
        if (!compromiso.getMatricula().getEstudiante().getApoderado().getId().equals(apoderado.getId())) {
            throw new ReglaNegocioException("El compromiso de pago no pertenece a ningún estudiante a su cargo.");
        }

        // 3. El compromiso debe seguir pendiente
        Matricula matricula = compromiso.getMatricula();
        if (compromiso.getEstado() == EstadoCompromisoPago.PAGADO
                || compromiso.getEstado() == EstadoCompromisoPago.CANCELADO) {
            throw new ReglaNegocioException(
                    "El compromiso de pago ya está pagado o cancelado; no admite nuevos pagos.");
        }

        // 4. Reglas de periodo para pagos de matrícula
        if (compromiso.getTipoCompromiso() == TipoCompromiso.MATRICULA) {
            if (matricula.getEstado() == EstadoMatricula.CANCELADA) {
                throw new ReglaNegocioException(
                        "No se puede registrar el pago porque la matrícula se encuentra CANCELADA.");
            }
            if (matricula.getEstado() == EstadoMatricula.CONFIRMADA) {
                throw new ReglaNegocioException(
                        "La matrícula ya está confirmada; no se requieren pagos adicionales.");
            }
            exigirPeriodoVigente(matricula, "No se puede registrar el pago: el período de matrícula ha finalizado.");
        } else {
            exigirAnioVigente(matricula,
                    "No se puede registrar el pago: el año académico ha finalizado.");
        }

        // 4. Validar que no exista un pago en revisión (PENDIENTE) para el mismo compromiso
        if (pagoRepository.existsByCompromisoPagoIdAndEstado(request.compromisoId(), EstadoPago.PENDIENTE)) {
            throw new ReglaNegocioException("Ya existe una solicitud de pago pendiente de validación para este compromiso.");
        }

        // 5. Guardar archivo en Cloudinary (valida PDF y 5 MB)
        String evidenciaUrl = servicioAlmacenamientoArchivo.guardarArchivo(request.comprobante(), "comprobantes/pagos");

        // 6. Persistir
        Pago pago = Pago.builder()
                .fechaRegistro(LocalDate.now())
                .montoPagado(request.montoPagado())
                .evidenciaUrl(evidenciaUrl)
                .estado(EstadoPago.PENDIENTE)
                .compromisoPago(compromiso)
                .apoderadoPagador(apoderado)
                .build();

        Pago pagoGuardado = pagoRepository.save(pago);

        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                null,
                CategoriaAuditoria.PAGOS,
                TipoAccionAuditoria.CREAR,
                "Pago",
                pagoGuardado.getId(),
                "Registro de pago",
                List.of(
                        new DetalleAuditoriaDTO("montoPagado", null, pagoGuardado.getMontoPagado().toPlainString()),
                        new DetalleAuditoriaDTO("compromisoId", null, String.valueOf(compromiso.getId())),
                        new DetalleAuditoriaDTO("estado", null, pagoGuardado.getEstado().name())
                )
        ));

        return mapToPagoApoderadoResponse(pagoGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PagoAdminListResponse> listarPagosAdmin(
            Long anioAcademicoId,
            Long gradoId,
            Long seccionId,
            Long estudianteId,
            EstadoPago estado,
            TipoCompromiso tipoCompromiso,
            Pageable pageable) {

        exigirAdministrador();

        return pagoRepository.listarPagosFiltrados(
                anioAcademicoId,
                gradoId,
                seccionId,
                estudianteId,
                estado,
                tipoCompromiso,
                pageable
        ).map(PagoAdminListResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompromisoPagoAdminResponse> listarPagosAgrupadosAdmin(
            Long anioAcademicoId,
            Long gradoId,
            Long seccionId,
            Long estudianteId,
            EstadoCompromisoPago estadoCompromiso,
            TipoCompromiso tipoCompromiso,
            Pageable pageable) {

        exigirAdministrador();

        Page<CompromisoPago> compromisos = compromisoPagoRepository.listarAdmin(
                anioAcademicoId,
                gradoId,
                seccionId,
                estudianteId,
                estadoCompromiso,
                tipoCompromiso,
                pageable
        );

        List<Long> compromisoIds = compromisos.getContent().stream()
                .map(CompromisoPago::getId)
                .toList();

        Map<Long, List<Pago>> pagosPorCompromiso = compromisoIds.isEmpty()
                ? Map.of()
                : pagoRepository.findByCompromisoPagoIdIn(compromisoIds).stream()
                        .collect(Collectors.groupingBy(
                                pago -> pago.getCompromisoPago().getId()));

        return compromisos.map(compromiso -> CompromisoPagoAdminResponse.fromEntity(
                compromiso,
                pagosPorCompromiso.getOrDefault(compromiso.getId(), List.of())
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PagoAdminDetailResponse obtenerDetallePagoAdmin(Long pagoId) {
        exigirAdministrador();
        Pago pago = pagoRepository.findByIdConDetalle(pagoId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pago", pagoId));

        return PagoAdminDetailResponse.fromEntity(pago);
    }

    @Override
    @Transactional
    public PagoAdminDetailResponse validarPagoAdmin(Long pagoId, ValidarPagoRequest request) {
        Administrador validador = exigirAdministrador();

        Pago pago = pagoRepository.findByIdConDetalle(pagoId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pago", pagoId));

        var compromiso = pago.getCompromisoPago();
        var matricula = compromiso.getMatricula();

        // Si ya está validado, la operación es idempotente.
        if (pago.getEstado() == EstadoPago.VALIDADO) {
            return PagoAdminDetailResponse.fromEntity(pago);
        }

        if (matricula.getEstado() == EstadoMatricula.CANCELADA
                || compromiso.getEstado() == EstadoCompromisoPago.CANCELADO) {
            throw new ReglaNegocioException(
                    "No se puede validar el pago porque el compromiso o la matrícula se encuentra CANCELADA.");
        }

        if (pagoRepository.existsByCompromisoPagoIdAndEstadoAndIdNot(
                compromiso.getId(), EstadoPago.VALIDADO, pago.getId())) {
            throw new ConflictoException(
                    "Ya existe otro pago validado para este compromiso.");
        }

        if (compromiso.getTipoCompromiso() == TipoCompromiso.MATRICULA) {
            exigirPeriodoVigente(matricula,
                    "El período de matrícula ha finalizado; ya no se puede validar un pago de matrícula.");
        } else {
            exigirAnioVigente(matricula,
                    "El año académico ha finalizado; ya no se puede validar un pago de pensión.");
        }

        String estadoPagoAnt = pago.getEstado().name();
        String estadoCompromisoAnt = compromiso.getEstado().name();
        String estadoMatriculaAnt = matricula.getEstado().name();

        pago.setEstado(EstadoPago.VALIDADO);
        pago.setUsuarioValidador(validador);
        compromiso.setEstado(EstadoCompromisoPago.PAGADO);

        if (compromiso.getTipoCompromiso() == TipoCompromiso.MATRICULA) {
            matricula.setEstado(EstadoMatricula.CONFIRMADA);
        }

        Pago pagoGuardado = pagoRepository.save(pago);

        var apoderado = pago.getApoderadoPagador();
        var estudiante = matricula.getEstudiante();

        servicioNotificacionEmail.enviarCorreoValidacionPago(
                apoderado.getEmail(),
                apoderado.getNombre() + " " + apoderado.getApellidoPaterno(),
                estudiante.getNombre() + " " + estudiante.getApellidoPaterno(),
                compromiso.getTipoCompromiso().name(),
                pago.getMontoPagado()
        );

        auditarCambioEstado(pagoGuardado, estadoPagoAnt, estadoCompromisoAnt, estadoMatriculaAnt,
                "Validación de pago por el administrador");

        return PagoAdminDetailResponse.fromEntity(pagoGuardado);
    }

    @Override
    @Transactional
    public PagoAdminDetailResponse rechazarPagoAdmin(Long pagoId, RechazarPagoRequest request) {
        Administrador validador = exigirAdministrador();

        Pago pago = pagoRepository.findByIdConDetalle(pagoId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pago", pagoId));

        var compromiso = pago.getCompromisoPago();
        var matricula = compromiso.getMatricula();

        // Si ya está rechazado, la operación es idempotente.
        if (pago.getEstado() == EstadoPago.RECHAZADO) {
            return PagoAdminDetailResponse.fromEntity(pago);
        }

        if (matricula.getEstado() == EstadoMatricula.CANCELADA
                || compromiso.getEstado() == EstadoCompromisoPago.CANCELADO) {
            throw new ReglaNegocioException(
                    "No se puede rechazar el pago porque el compromiso o la matrícula se encuentra CANCELADA.");
        }

        if (compromiso.getTipoCompromiso() == TipoCompromiso.MATRICULA) {
            exigirPeriodoVigente(matricula,
                    "El período de matrícula ha finalizado; ya no se puede rechazar un pago de matrícula.");
        } else {
            exigirAnioVigente(matricula,
                    "El año académico ha finalizado; ya no se puede rechazar un pago de pensión.");
        }

        boolean existeOtroValidado = pagoRepository.existsByCompromisoPagoIdAndEstadoAndIdNot(
                compromiso.getId(), EstadoPago.VALIDADO, pago.getId());

        String estadoPagoAnt = pago.getEstado().name();
        String estadoCompromisoAnt = compromiso.getEstado().name();
        String estadoMatriculaAnt = matricula.getEstado().name();

        pago.setEstado(EstadoPago.RECHAZADO);
        pago.setMotivo(request.motivo());
        pago.setUsuarioValidador(validador);

        // Si queda otro pago validado, el compromiso y la matrícula siguen pagados.
        if (!existeOtroValidado) {
            compromiso.setEstado(EstadoCompromisoPago.PENDIENTE);
            if (compromiso.getTipoCompromiso() == TipoCompromiso.MATRICULA) {
                matricula.setEstado(EstadoMatricula.PENDIENTE);
            }
        }

        Pago pagoGuardado = pagoRepository.save(pago);

        var apoderado = pago.getApoderadoPagador();
        var estudiante = matricula.getEstudiante();

        servicioNotificacionEmail.enviarCorreoRechazoPago(
                apoderado.getEmail(),
                apoderado.getNombre() + " " + apoderado.getApellidoPaterno(),
                estudiante.getNombre() + " " + estudiante.getApellidoPaterno(),
                compromiso.getTipoCompromiso().name(),
                request.motivo()
        );

        auditarCambioEstado(pagoGuardado, estadoPagoAnt, estadoCompromisoAnt, estadoMatriculaAnt,
                "Rechazo de pago por el administrador. Motivo: " + request.motivo());

        return PagoAdminDetailResponse.fromEntity(pagoGuardado);
    }

    // --- Seguridad, periodo y auditoría ---

    private void exigirPeriodoVigente(Matricula matricula, String mensaje) {
        Long anioId = matricula.getSeccion().getAnioAcademico().getId();
        PeriodoMatricula periodo = periodoMatriculaRepository.findByAnioAcademicoId(anioId)
                .orElseThrow(() -> new ReglaNegocioException("No se encontró período de matrícula para el año académico."));
        if (!periodo.estaVigente(LocalDate.now())) {
            throw new ReglaNegocioException(mensaje);
        }
    }

    private void exigirAnioVigente(Matricula matricula, String mensaje) {
        AnioAcademico anio = matricula.getSeccion().getAnioAcademico();
        if (anio == null || !anio.estaVigente()) {
            throw new ReglaNegocioException(mensaje);
        }
    }

    private Administrador exigirAdministrador() {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (!(usuario instanceof Administrador administrador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un administrador puede realizar esta operación.");
        }
        return administrador;
    }

    private Apoderado exigirApoderado() {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (!(usuario instanceof Apoderado apoderado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un apoderado puede realizar esta operación.");
        }
        return apoderado;
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return usuario;
    }

    private void auditarCambioEstado(Pago pago, String estadoPagoAnt, String estadoCompromisoAnt,
                                     String estadoMatriculaAnt, String descripcion) {
        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                null,
                CategoriaAuditoria.PAGOS,
                TipoAccionAuditoria.CAMBIO_ESTADO,
                "Pago",
                pago.getId(),
                descripcion,
                List.of(
                        new DetalleAuditoriaDTO("estadoPago", estadoPagoAnt, pago.getEstado().name()),
                        new DetalleAuditoriaDTO("estadoCompromiso", estadoCompromisoAnt,
                                pago.getCompromisoPago().getEstado().name()),
                        new DetalleAuditoriaDTO("estadoMatricula", estadoMatriculaAnt,
                                pago.getCompromisoPago().getMatricula().getEstado().name())
                )
        ));
    }

    private PagoApoderadoResponse mapToPagoApoderadoResponse(Pago p) {
        return new PagoApoderadoResponse(
                p.getId(),
                p.getFechaRegistro(),
                p.getMontoPagado(),
                p.getEvidenciaUrl(),
                p.getEstado(),
                p.getMotivo(),
                p.getCompromisoPago().getId(),
                p.getApoderadoPagador().getId()
        );
    }
}