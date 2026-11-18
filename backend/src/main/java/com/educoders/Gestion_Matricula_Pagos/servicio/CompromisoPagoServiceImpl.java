package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.CompromisoPagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoApoderadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoPagoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoPensionRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosMatriculaLoteRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosMatriculaLoteResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosPensionLoteRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosPensionLoteResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmisionCompromisoLoteError;
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.Mes;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.operaciones.ProgresoLote;
import com.educoders.Infraestructura_Servicios.servicio.ServicioNotificacionEmail;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleAuditoriaDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CompromisoPagoServiceImpl implements CompromisoPagoService {

    private static final Logger log = LoggerFactory.getLogger(CompromisoPagoServiceImpl.class);

    @Value("${app.pagos.monto-matricula:350.00}")
    private BigDecimal montoMatriculaPredeterminado = new BigDecimal("350.00");

    @Value("${app.pagos.monto-pension:450.00}")
    private BigDecimal montoPensionPredeterminado = new BigDecimal("450.00");

    private final CompromisoPagoRepository compromisoPagoRepository;
    private final MatriculaRepository matriculaRepository;
    private final PeriodoMatriculaRepository periodoMatriculaRepository;
    private final AnioAcademicoRepository anioAcademicoRepository;
    private final ServicioNotificacionEmail servicioNotificacionEmail;
    private final EstudianteRepository estudianteRepository;
    private final AuditoriaService auditoriaService;
    private final TransactionTemplate transaccionItem;

    public CompromisoPagoServiceImpl(CompromisoPagoRepository compromisoPagoRepository,
                                     MatriculaRepository matriculaRepository,
                                     PeriodoMatriculaRepository periodoMatriculaRepository,
                                     AnioAcademicoRepository anioAcademicoRepository,
                                     ServicioNotificacionEmail servicioNotificacionEmail,
                                     EstudianteRepository estudianteRepository,
                                     AuditoriaService auditoriaService,
                                     PlatformTransactionManager transactionManager) {
        this.compromisoPagoRepository = compromisoPagoRepository;
        this.matriculaRepository = matriculaRepository;
        this.periodoMatriculaRepository = periodoMatriculaRepository;
        this.anioAcademicoRepository = anioAcademicoRepository;
        this.servicioNotificacionEmail = servicioNotificacionEmail;
        this.estudianteRepository = estudianteRepository;
        this.auditoriaService = auditoriaService;
        this.transaccionItem = new TransactionTemplate(transactionManager);
        this.transaccionItem.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    @Transactional
    public CompromisoPagoResponse emitirCompromisoMatricula(EmitirCompromisoMatriculaRequest request) {
        exigirAdministrador();
        LocalDate hoy = LocalDate.now();

        Matricula matricula = resolverMatricula(
                request.matriculaId(),
                request.estudianteId(),
                request.anioId(),
                EstadoMatricula.PENDIENTE
        );

        AnioAcademico anioAcademico = matricula.getSeccion().getAnioAcademico();
        PeriodoMatricula periodo = periodoMatriculaRepository.findByAnioAcademicoId(anioAcademico.getId())
                .orElseThrow(() -> new ReglaNegocioException("No se encontró período de matrícula habilitado para el año académico."));

        if (!periodo.estaVigente(hoy)) {
            throw new ReglaNegocioException("El período de matrícula ha finalizado o no se encuentra vigente.");
        }

        boolean yaEmitido = compromisoPagoRepository.existsByMatriculaIdAndTipoCompromiso(
                matricula.getId(), TipoCompromiso.MATRICULA);
        if (yaEmitido) {
            throw new ConflictoException("El compromiso de pago de matrícula ya fue emitido previamente para esta matrícula.");
        }

        BigDecimal montoFinal = montoMatriculaPredeterminado;

        CompromisoPago compromiso = CompromisoPago.builder()
                .tipoCompromiso(TipoCompromiso.MATRICULA)
                .mes(null)
                .fechaEmision(hoy)
                .montoOriginal(montoFinal)
                .montoMora(BigDecimal.ZERO)
                .estado(EstadoCompromisoPago.PENDIENTE)
                .matricula(matricula)
                .build();

        CompromisoPago guardado = compromisoPagoRepository.save(compromiso);

        Estudiante est = matricula.getEstudiante();
        Apoderado apod = est.getApoderado();
        if (apod != null) {
            servicioNotificacionEmail.enviarCorreoCompromisoPago(
                    apod.getEmail(),
                    apod.getNombre() + " " + apod.getApellidoPaterno(),
                    est.getNombre() + " " + est.getApellidoPaterno(),
                    "Matrícula Escolar",
                    guardado.calcularMontoTotal()
            );
        }

        auditarCompromiso(guardado, "Emisión de compromiso de matrícula");

        return mapToResponse(guardado);
    }

    @Override
    public EmitirCompromisosMatriculaLoteResponse emitirCompromisosMatriculaLote(
            EmitirCompromisosMatriculaLoteRequest request) {
        return emitirCompromisosMatriculaLote(request, null, null);
    }

    @Override
    public EmitirCompromisosMatriculaLoteResponse emitirCompromisosMatriculaLote(
            EmitirCompromisosMatriculaLoteRequest request,
            Long usuarioId,
            ProgresoLote progreso) {

        if (usuarioId == null) {
            exigirAdministrador();
        }

        if (request == null || request.anioId() == null) {
            throw new ReglaNegocioException("El año académico es obligatorio.");
        }

        PeriodoMatricula periodo = periodoMatriculaRepository
                .findByAnioAcademicoId(request.anioId())
                .orElseThrow(() -> RecursoNoEncontradoException.de(
                        "Período de matrícula del año", request.anioId()));

        if (!periodo.estaVigente(LocalDate.now())) {
            throw new ReglaNegocioException(
                    "El período de matrícula ha finalizado o no se encuentra vigente.");
        }

        List<Matricula> pendientes = matriculaRepository.buscarPendientesPorAnioConDetalle(
                request.anioId(), EstadoMatricula.PENDIENTE);

        int emitidos = 0;
        int omitidos = 0;
        int procesados = 0;
        List<EmisionCompromisoLoteError> errores = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        if (progreso != null) {
            progreso.iniciar(pendientes.size());
        }

        for (Matricula matricula : pendientes) {
            try {
                boolean yaEmitido = compromisoPagoRepository.existsByMatriculaIdAndTipoCompromiso(
                        matricula.getId(), TipoCompromiso.MATRICULA);
                if (yaEmitido) {
                    omitidos++;
                    continue;
                }

                CompromisoPago guardado = transaccionItem.execute(status -> {
                    CompromisoPago compromiso = CompromisoPago.builder()
                            .tipoCompromiso(TipoCompromiso.MATRICULA)
                            .mes(null)
                            .fechaEmision(hoy)
                            .montoOriginal(montoMatriculaPredeterminado)
                            .montoMora(BigDecimal.ZERO)
                            .estado(EstadoCompromisoPago.PENDIENTE)
                            .matricula(matricula)
                            .build();

                    CompromisoPago creado = compromisoPagoRepository.save(compromiso);
                    auditarCompromiso(creado,
                            "Emisión masiva de compromiso de matrícula", usuarioId);
                    return creado;
                });

                notificarCompromiso(guardado, "Matrícula Escolar");
                emitidos++;
            } catch (Exception e) {
                String mensaje = e.getMessage() != null
                        ? e.getMessage()
                        : e.getClass().getSimpleName();
                errores.add(new EmisionCompromisoLoteError(matricula.getId(), mensaje));
                if (progreso != null) {
                    progreso.registrarError(
                            "Matrícula " + matricula.getId() + ": " + mensaje);
                }
            } finally {
                procesados++;
                if (progreso != null) {
                    progreso.actualizar(procesados, emitidos, omitidos, errores.size());
                }
            }
        }

        return new EmitirCompromisosMatriculaLoteResponse(emitidos, omitidos, errores);
    }

    @Override
    public EmitirCompromisosPensionLoteResponse emitirCompromisosPensionLote(
            EmitirCompromisosPensionLoteRequest request) {
        return emitirCompromisosPensionLote(request, null, null);
    }

    @Override
    public EmitirCompromisosPensionLoteResponse emitirCompromisosPensionLote(
            EmitirCompromisosPensionLoteRequest request,
            Long usuarioId,
            ProgresoLote progreso) {

        if (usuarioId == null) {
            exigirAdministrador();
        }

        if (request == null || request.anioId() == null || request.mes() == null) {
            throw new ReglaNegocioException("El año académico y el mes son obligatorios.");
        }

        AnioAcademico anio = anioAcademicoRepository.findById(request.anioId())
                .orElseThrow(() -> RecursoNoEncontradoException.de(
                        "Año académico", request.anioId()));

        LocalDate hoy = LocalDate.now();
        exigirVentanaPension(anio, hoy);

        int numeroMes = request.mes().ordinal() + 3;
        if (numeroMes > hoy.getMonthValue()) {
            throw new ReglaNegocioException(
                    "No se pueden emitir compromisos de pensión de meses futuros.");
        }

        List<Matricula> matriculas = matriculaRepository.buscarPorAnioYEstadoConDetalle(
                request.anioId(),
                EstadoMatricula.CONFIRMADA,
                request.gradoId(),
                request.seccionId()
        );

        exigirMesAnteriorEnLote(request.mes(), matriculas);

        int emitidos = 0;
        int omitidos = 0;
        int procesados = 0;
        List<EmisionCompromisoLoteError> errores = new ArrayList<>();

        if (progreso != null) {
            progreso.iniciar(matriculas.size());
        }

        for (Matricula matricula : matriculas) {
            try {
                boolean yaEmitido = compromisoPagoRepository
                        .existsByMatriculaIdAndTipoCompromisoAndMes(
                                matricula.getId(), TipoCompromiso.PENSION, request.mes());
                if (yaEmitido) {
                    omitidos++;
                    continue;
                }

                CompromisoPago guardado = transaccionItem.execute(status -> {
                    CompromisoPago compromiso = CompromisoPago.builder()
                            .tipoCompromiso(TipoCompromiso.PENSION)
                            .mes(request.mes())
                            .fechaEmision(hoy)
                            .montoOriginal(montoPensionPredeterminado)
                            .montoMora(BigDecimal.ZERO)
                            .estado(EstadoCompromisoPago.PENDIENTE)
                            .matricula(matricula)
                            .build();

                    CompromisoPago creado = compromisoPagoRepository.save(compromiso);
                    auditarCompromiso(creado,
                            "Emisión masiva de compromiso de pensión", usuarioId);
                    return creado;
                });

                notificarCompromiso(guardado, "Pensión Mensual - " + request.mes());
                emitidos++;
            } catch (Exception e) {
                String mensaje = e.getMessage() != null
                        ? e.getMessage()
                        : e.getClass().getSimpleName();
                errores.add(new EmisionCompromisoLoteError(matricula.getId(), mensaje));
                if (progreso != null) {
                    progreso.registrarError(
                            "Matrícula " + matricula.getId() + ": " + mensaje);
                }
            } finally {
                procesados++;
                if (progreso != null) {
                    progreso.actualizar(procesados, emitidos, omitidos, errores.size());
                }
            }
        }

        return new EmitirCompromisosPensionLoteResponse(emitidos, omitidos, errores);
    }

    @Override
    @Transactional
    public CompromisoPagoResponse emitirCompromisoPension(EmitirCompromisoPensionRequest request) {
        exigirAdministrador();
        LocalDate hoy = LocalDate.now();

        Matricula matricula = resolverMatricula(
                request.matriculaId(),
                request.estudianteId(),
                request.anioId(),
                EstadoMatricula.CONFIRMADA
        );

        AnioAcademico anio = matricula.getSeccion().getAnioAcademico();
        exigirVentanaPension(anio, hoy);

        if (request.mes() == null) {
            throw new ReglaNegocioException("El mes es obligatorio.");
        }
        int numeroMes = request.mes().ordinal() + 3; // MARZO = 3
        if (numeroMes > hoy.getMonthValue()) {
            throw new ReglaNegocioException("No se pueden emitir compromisos de pensión de meses futuros.");
        }

        Mes anterior = mesAnterior(request.mes());
        if (anterior != null
                && !compromisoPagoRepository.existsByMatriculaIdAndTipoCompromisoAndMes(
                        matricula.getId(), TipoCompromiso.PENSION, anterior)) {
            throw new ReglaNegocioException(
                    "No se puede emitir el compromiso de " + request.mes()
                            + " porque falta el compromiso de " + anterior
                            + " para esta matrícula.");
        }

        boolean yaEmitido = compromisoPagoRepository.existsByMatriculaIdAndTipoCompromisoAndMes(
                matricula.getId(), TipoCompromiso.PENSION, request.mes());
        if (yaEmitido) {
            throw new ConflictoException(
                    "Ya existe un compromiso de pago de pensión emitido para el mes de " + request.mes() + " en esta matrícula.");
        }

        BigDecimal montoFinal = montoPensionPredeterminado;

        CompromisoPago compromiso = CompromisoPago.builder()
                .tipoCompromiso(TipoCompromiso.PENSION)
                .mes(request.mes())
                .fechaEmision(hoy)
                .montoOriginal(montoFinal)
                .montoMora(BigDecimal.ZERO)
                .estado(EstadoCompromisoPago.PENDIENTE)
                .matricula(matricula)
                .build();

        CompromisoPago guardado = compromisoPagoRepository.save(compromiso);

        Estudiante est = matricula.getEstudiante();
        Apoderado apod = est.getApoderado();
        if (apod != null) {
            servicioNotificacionEmail.enviarCorreoCompromisoPago(
                    apod.getEmail(),
                    apod.getNombre() + " " + apod.getApellidoPaterno(),
                    est.getNombre() + " " + est.getApellidoPaterno(),
                    "Pensión Mensual - " + request.mes(),
                    guardado.calcularMontoTotal()
            );
        }

        auditarCompromiso(guardado, "Emisión de compromiso de pensión");

        return mapToResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompromisoApoderadoResponse> listarCompromisosPorEstudianteYAnio(Long estudianteId, Integer anio) {
        Apoderado apoderado = exigirApoderado();

        if (!estudianteRepository.existsByIdAndApoderadoId(estudianteId, apoderado.getId())) {
            throw new ReglaNegocioException("El estudiante especificado no se encuentra asociado al apoderado autenticado.");
        }

        LocalDate hoy = LocalDate.now();
        return compromisoPagoRepository.findByEstudianteAndAnioOpcional(estudianteId, anio)
                .stream()
                .map(c -> mapToCompromisoApoderadoResponse(c, hoy))
                .toList();
    }

    // --- Resolución de la matrícula ---

    /**
     * Resuelve la matrícula desde {@code matriculaId} (compatibilidad) o desde
     * {@code estudianteId} + {@code anioId} (nuevo contrato). En el segundo caso
     * exige que la matrícula esté en el estado esperado.
     */
    private Matricula resolverMatricula(Long matriculaId,
                                        Long estudianteId,
                                        Long anioId,
                                        EstadoMatricula estadoEsperado) {
        if (matriculaId != null) {
            Matricula matricula = matriculaRepository.findById(matriculaId)
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Matrícula", matriculaId));
            validarEstadoMatricula(matricula, estadoEsperado);
            return matricula;
        }

        if (estudianteId == null || anioId == null) {
            throw new ReglaNegocioException(
                    "Debe indicar matriculaId o, en su defecto, estudianteId junto con anioId.");
        }

        if (!estudianteRepository.existsById(estudianteId)) {
            throw RecursoNoEncontradoException.de("Estudiante", estudianteId);
        }

        return matriculaRepository
                .buscarPorEstudianteYAnioId(estudianteId, anioId, estadoEsperado)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "No se encontró una matrícula en estado " + estadoEsperado
                                + " para el estudiante en el año académico indicado."));
    }

    private void validarEstadoMatricula(Matricula matricula, EstadoMatricula estadoEsperado) {
        if (estadoEsperado == EstadoMatricula.PENDIENTE && !matricula.esPendiente()) {
            throw new ReglaNegocioException(
                    "Solo se pueden emitir compromisos de pago para matrículas en estado Pendiente.");
        }
        if (estadoEsperado == EstadoMatricula.CONFIRMADA && !matricula.esConfirmada()) {
            throw new ReglaNegocioException(
                    "El estudiante no tiene una matrícula confirmada para emitir compromisos de pensión.");
        }
    }

    // --- Auditoría y seguridad ---

    /**
     * Envía la notificación después del commit de la matrícula. El servicio de
     * correo ya captura sus propios errores, así que un fallo de notificación
     * no revierte ni marca como fallida una emisión que sí quedó guardada.
     */
    private void notificarCompromiso(CompromisoPago compromiso, String concepto) {
        if (compromiso == null) {
            return;
        }

        try {
            Estudiante estudiante = compromiso.getMatricula().getEstudiante();
            Apoderado apoderado = estudiante.getApoderado();
            if (apoderado == null) {
                return;
            }

            servicioNotificacionEmail.enviarCorreoCompromisoPago(
                    apoderado.getEmail(),
                    apoderado.getNombre() + " " + apoderado.getApellidoPaterno(),
                    estudiante.getNombre() + " " + estudiante.getApellidoPaterno(),
                    concepto,
                    compromiso.calcularMontoTotal()
            );
        } catch (Exception e) {
            log.warn("No se pudo notificar el compromiso {}: {}",
                    compromiso.getId(), e.getMessage());
        }
    }

    private void auditarCompromiso(CompromisoPago c, String descripcion) {
        auditarCompromiso(c, descripcion, null);
    }

    private void auditarCompromiso(CompromisoPago c, String descripcion, Long usuarioId) {
        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                usuarioId,
                CategoriaAuditoria.PAGOS,
                TipoAccionAuditoria.CREAR,
                "CompromisoPago",
                c.getId(),
                descripcion,
                List.of(
                        new DetalleAuditoriaDTO("tipoCompromiso", null, c.getTipoCompromiso().name()),
                        new DetalleAuditoriaDTO("mes", null, c.getMes() != null ? c.getMes().name() : null),
                        new DetalleAuditoriaDTO("montoOriginal", null, c.getMontoOriginal().toPlainString()),
                        new DetalleAuditoriaDTO("matriculaId", null, String.valueOf(c.getMatricula().getId())),
                        new DetalleAuditoriaDTO("estado", null, c.getEstado().name())
                )
        ));
    }

    private void exigirAdministrador() {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (!(usuario instanceof Administrador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un administrador puede emitir compromisos de pago.");
        }
    }

    private Apoderado exigirApoderado() {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (!(usuario instanceof Apoderado apoderado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un apoderado puede consultar sus compromisos de pago.");
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

    /**
     * Ventana de emisión de pensiones: empieza el día siguiente al fin del
     * periodo de matrícula y termina con el año académico.
     */
    private void exigirVentanaPension(AnioAcademico anio, LocalDate hoy) {
        PeriodoMatricula periodo = periodoMatriculaRepository
                .findByAnioAcademicoId(anio.getId())
                .orElseThrow(() -> new ReglaNegocioException(
                        "No existe un periodo de matrícula para el año académico "
                                + anio.getAnio() + "."));

        if (periodo.getFechaFin() == null || !hoy.isAfter(periodo.getFechaFin())) {
            throw new ReglaNegocioException(
                    "No se pueden emitir compromisos de pensión mientras el periodo "
                            + "de matrícula esté vigente. Debe esperar a que finalice el "
                            + (periodo.getFechaFin() != null ? periodo.getFechaFin() : "periodo") + ".");
        }

        if (anio.getFechaFin() == null) {
            throw new ReglaNegocioException(
                    "El año académico no tiene fecha de fin configurada; "
                            + "no se pueden emitir compromisos de pensión.");
        }
        if (hoy.isAfter(anio.getFechaFin())) {
            throw new ReglaNegocioException(
                    "El año académico ya finalizó; no se pueden emitir compromisos de pensión.");
        }
    }

    private Mes mesAnterior(Mes mes) {
        return mes == Mes.MARZO ? null : Mes.values()[mes.ordinal() - 1];
    }

    /**
     * En la emisión masiva se exige que todas las matrículas objetivo tengan el
     * compromiso del mes anterior antes de crear cualquier compromiso nuevo.
     */
    private void exigirMesAnteriorEnLote(Mes mes, List<Matricula> matriculas) {
        Mes anterior = mesAnterior(mes);
        if (anterior == null || matriculas.isEmpty()) {
            return;
        }

        List<Long> ids = matriculas.stream().map(Matricula::getId).toList();
        Set<Long> conAnterior = new HashSet<>(compromisoPagoRepository
                .findMatriculaIdsConCompromiso(ids, TipoCompromiso.PENSION, anterior));

        List<Long> faltantes = ids.stream()
                .filter(id -> !conAnterior.contains(id))
                .toList();

        if (faltantes.isEmpty()) {
            return;
        }

        StringBuilder detalle = new StringBuilder();
        for (int i = 0; i < Math.min(faltantes.size(), 10); i++) {
            if (i > 0) {
                detalle.append(", ");
            }
            detalle.append(faltantes.get(i));
        }
        if (faltantes.size() > 10) {
            detalle.append(", ...");
        }

        throw new ReglaNegocioException(
                "No se puede emitir " + mes + " porque " + faltantes.size()
                        + " matrícula(s) no tienen el compromiso de " + anterior
                        + ": " + detalle + ".");
    }

    // --- Mapeos ---

    private CompromisoApoderadoResponse mapToCompromisoApoderadoResponse(CompromisoPago c, LocalDate hoy) {
        Matricula m = c.getMatricula();
        boolean esCancelada = m.esCancelada();
        String etiquetaMatricula = esCancelada ? "Matrícula cancelada" : null;

        ResultadoEvaluacionBloqueo evaluacion = evaluarHabilitacionPago(c, hoy);

        return new CompromisoApoderadoResponse(
                c.getId(),
                c.getTipoCompromiso(),
                c.getMes(),
                c.getFechaEmision(),
                c.getMontoOriginal(),
                c.getMontoMora(),
                c.calcularMontoTotal(),
                c.getEstado(),
                m.getId(),
                esCancelada,
                etiquetaMatricula,
                evaluacion.sePuedeRegistrarPago(),
                evaluacion.motivoBloqueo()
        );
    }

    private ResultadoEvaluacionBloqueo evaluarHabilitacionPago(CompromisoPago c, LocalDate hoy) {
        if (c.esPagado()) {
            return new ResultadoEvaluacionBloqueo(false, "El compromiso ya se encuentra pagado.");
        }

        Matricula matricula = c.getMatricula();
        if (matricula.esCancelada()) {
            return new ResultadoEvaluacionBloqueo(false, "La matrícula del estudiante ha sido cancelada.");
        }

        if (c.getTipoCompromiso() == TipoCompromiso.MATRICULA) {
            Long anioAcademicoId = matricula.getSeccion().getAnioAcademico().getId();
            boolean periodoVigente = periodoMatriculaRepository.findByAnioAcademicoId(anioAcademicoId)
                    .map(periodo -> periodo.estaVigente(hoy))
                    .orElse(false);

            if (!periodoVigente) {
                return new ResultadoEvaluacionBloqueo(false, "El período de matrícula ha finalizado o no se encuentra vigente.");
            }
        }

        return new ResultadoEvaluacionBloqueo(true, null);
    }

    private record ResultadoEvaluacionBloqueo(boolean sePuedeRegistrarPago, String motivoBloqueo) {}

    private CompromisoPagoResponse mapToResponse(CompromisoPago c) {
        Estudiante est = c.getMatricula().getEstudiante();
        Apoderado apod = est.getApoderado();

        String nombreEstudiante = est.getNombre() + " " + est.getApellidoPaterno();
        String nombreApoderado = apod.getNombre() + " " + apod.getApellidoPaterno();

        return new CompromisoPagoResponse(
                c.getId(),
                c.getTipoCompromiso(),
                c.getMes(),
                c.getFechaEmision(),
                c.getMontoOriginal(),
                c.getMontoMora(),
                c.calcularMontoTotal(),
                c.getEstado(),
                c.getMatricula().getId(),
                est.getId(),
                nombreEstudiante,
                nombreApoderado
        );
    }
}