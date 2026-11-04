package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
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
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.servicio.ServicioNotificacionEmail;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleAuditoriaDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CompromisoPagoServiceImpl implements CompromisoPagoService {

    private static final BigDecimal MONTO_MATRICULA_PREDETERMINADO = new BigDecimal("350.00");
    private static final BigDecimal MONTO_PENSION_PREDETERMINADO = new BigDecimal("450.00");

    private final CompromisoPagoRepository compromisoPagoRepository;
    private final MatriculaRepository matriculaRepository;
    private final PeriodoMatriculaRepository periodoMatriculaRepository;
    private final ServicioNotificacionEmail servicioNotificacionEmail;
    private final EstudianteRepository estudianteRepository;
    private final AuditoriaService auditoriaService;

    public CompromisoPagoServiceImpl(CompromisoPagoRepository compromisoPagoRepository,
                                     MatriculaRepository matriculaRepository,
                                     PeriodoMatriculaRepository periodoMatriculaRepository,
                                     ServicioNotificacionEmail servicioNotificacionEmail,
                                     EstudianteRepository estudianteRepository,
                                     AuditoriaService auditoriaService) {
        this.compromisoPagoRepository = compromisoPagoRepository;
        this.matriculaRepository = matriculaRepository;
        this.periodoMatriculaRepository = periodoMatriculaRepository;
        this.servicioNotificacionEmail = servicioNotificacionEmail;
        this.estudianteRepository = estudianteRepository;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public CompromisoPagoResponse emitirCompromisoMatricula(EmitirCompromisoMatriculaRequest request) {
        exigirAdministrador();
        LocalDate hoy = LocalDate.now();

        Matricula matricula = matriculaRepository.findById(request.matriculaId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Matrícula", request.matriculaId()));

        if (!matricula.esPendiente()) {
            throw new ReglaNegocioException("Solo se pueden emitir compromisos de pago para matrículas en estado Pendiente.");
        }

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

        BigDecimal montoFinal = (request.montoPersonalizado() != null && request.montoPersonalizado().compareTo(BigDecimal.ZERO) > 0)
                ? request.montoPersonalizado()
                : MONTO_MATRICULA_PREDETERMINADO;

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
    @Transactional
    public CompromisoPagoResponse emitirCompromisoPension(EmitirCompromisoPensionRequest request) {
        exigirAdministrador();
        LocalDate hoy = LocalDate.now();

        Matricula matricula = matriculaRepository.findById(request.matriculaId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Matrícula", request.matriculaId()));

        if (matricula.getEstado() != EstadoMatricula.CONFIRMADA) {
            throw new ReglaNegocioException(
                    "El estudiante no tiene una matrícula confirmada para emitir compromisos de pensión.");
        }

        AnioAcademico anio = matricula.getSeccion().getAnioAcademico();
        if (!anioVigente(anio, hoy)) {
            throw new ReglaNegocioException("No hay un año académico vigente para emitir compromisos de pensión.");
        }

        if (request.mes() == null) {
            throw new ReglaNegocioException("El mes es obligatorio.");
        }
        int numeroMes = request.mes().ordinal() + 3; // MARZO = 3
        if (numeroMes > hoy.getMonthValue()) {
            throw new ReglaNegocioException("No se pueden emitir compromisos de pensión de meses futuros.");
        }

        boolean yaEmitido = compromisoPagoRepository.existsByMatriculaIdAndTipoCompromisoAndMes(
                matricula.getId(), TipoCompromiso.PENSION, request.mes());
        if (yaEmitido) {
            throw new ConflictoException(
                    "Ya existe un compromiso de pago de pensión emitido para el mes de " + request.mes() + " en esta matrícula.");
        }

        BigDecimal montoFinal = (request.montoPersonalizado() != null && request.montoPersonalizado().compareTo(BigDecimal.ZERO) > 0)
                ? request.montoPersonalizado()
                : MONTO_PENSION_PREDETERMINADO;

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

    // --- Auditoría y seguridad ---

    private void auditarCompromiso(CompromisoPago c, String descripcion) {
        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                null,
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

    private boolean anioVigente(AnioAcademico anio, LocalDate hoy) {
        if (anio.getFechaInicio() == null || hoy.isBefore(anio.getFechaInicio())) {
            return false;
        }
        return anio.getFechaFin() == null || !hoy.isAfter(anio.getFechaFin());
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