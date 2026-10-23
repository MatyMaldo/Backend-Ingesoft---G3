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
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.servicio.ServicioNotificacionEmail;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;

import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CompromisoPagoServiceImpl implements CompromisoPagoService {

    private static final BigDecimal MONTO_MATRICULA_PREDETERMINADO = new BigDecimal("350.00");
    private static final BigDecimal MONTO_PENSION_PREDETERMINADO = new BigDecimal("450.00");

    private final CompromisoPagoRepository compromisoPagoRepository;
    private final MatriculaRepository matriculaRepository;
    private final PeriodoMatriculaRepository periodoMatriculaRepository;
    private final ServicioNotificacionEmail servicioNotificacionEmail;
    private final EstudianteRepository estudianteRepository;

    public CompromisoPagoServiceImpl(CompromisoPagoRepository compromisoPagoRepository,
                                     MatriculaRepository matriculaRepository,
                                     PeriodoMatriculaRepository periodoMatriculaRepository,
                                     ServicioNotificacionEmail servicioNotificacionEmail, EstudianteRepository estudianteRepository) {
        this.compromisoPagoRepository = compromisoPagoRepository;
        this.matriculaRepository = matriculaRepository;
        this.periodoMatriculaRepository = periodoMatriculaRepository;
        this.servicioNotificacionEmail = servicioNotificacionEmail;
        this.estudianteRepository = estudianteRepository;
    }

    @Override
    @Transactional
    public CompromisoPagoResponse emitirCompromisoMatricula(EmitirCompromisoMatriculaRequest request) {
        LocalDate hoy = LocalDate.now();

        // 1. Obtener la matrícula existente
        Matricula matricula = matriculaRepository.findById(request.matriculaId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Matrícula", request.matriculaId()));

        // 2. Validar que la matrícula esté en estado PENDIENTE
        if (!matricula.esPendiente()) {
            throw new ReglaNegocioException("Solo se pueden emitir compromisos de pago para matrículas en estado Pendiente.");
        }

        // 3. Validar que el período de matrícula para ese año académico continúe vigente
        AnioAcademico anioAcademico = matricula.getSeccion().getAnioAcademico();
        PeriodoMatricula periodo = periodoMatriculaRepository.findByAnioAcademicoId(anioAcademico.getId())
                .orElseThrow(() -> new ReglaNegocioException("No se encontró período de matrícula habilitado para el año académico."));

        if (!periodo.estaVigente(hoy)) {
            throw new ReglaNegocioException("El período de matrícula ha finalizado o no se encuentra vigente.");
        }

        // 4. Validar que no se haya emitido previamente el compromiso de matrícula
        boolean yaEmitido = compromisoPagoRepository.existsByMatriculaIdAndTipoCompromiso(
                matricula.getId(),
                TipoCompromiso.MATRICULA
        );

        if (yaEmitido) {
            throw new ConflictoException("El compromiso de pago de matrícula ya fue emitido previamente para esta matrícula.");
        }

        // 5. Asignar monto original (personalizado o predeterminado)
        BigDecimal montoFinal = (request.montoPersonalizado() != null && request.montoPersonalizado().compareTo(BigDecimal.ZERO) > 0)
                ? request.montoPersonalizado()
                : MONTO_MATRICULA_PREDETERMINADO;

        // 6. Construir y guardar entidad de compromiso de matrícula
        CompromisoPago compromiso = CompromisoPago.builder()
                .tipoCompromiso(TipoCompromiso.MATRICULA)
                .mes(null) // Para matrículas el campo mes es null por BD
                .fechaEmision(hoy)
                .montoOriginal(montoFinal)
                .montoMora(BigDecimal.ZERO)
                .estado(EstadoCompromisoPago.PENDIENTE)
                .matricula(matricula)
                .build();

        CompromisoPago guardado = compromisoPagoRepository.save(compromiso);

        // 7. Notificación por correo electrónico al apoderado
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

        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public CompromisoPagoResponse emitirCompromisoPension(EmitirCompromisoPensionRequest request) {
        LocalDate hoy = LocalDate.now();

        // 1. Obtener la matrícula existente
        Matricula matricula = matriculaRepository.findById(request.matriculaId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Matrícula", request.matriculaId()));

        // 2. Validar que la matrícula esté vigente (no cancelada)
        if (matricula.esCancelada()) {
            throw new ReglaNegocioException("No se pueden emitir compromisos de pensión para una matrícula cancelada.");
        }

        // 3. Validar duplicidad de pensión para el mismo mes
        boolean yaEmitido = compromisoPagoRepository.existsByMatriculaIdAndTipoCompromisoAndMes(
                matricula.getId(),
                TipoCompromiso.PENSION,
                request.mes()
        );

        if (yaEmitido) {
            throw new ConflictoException(
                    "Ya existe un compromiso de pago de pensión emitido para el mes de " + request.mes() + " en esta matrícula.");
        }

        // 4. Asignar monto original (personalizado o predeterminado)
        BigDecimal montoFinal = (request.montoPersonalizado() != null && request.montoPersonalizado().compareTo(BigDecimal.ZERO) > 0)
                ? request.montoPersonalizado()
                : MONTO_PENSION_PREDETERMINADO;

        // 5. Construir y guardar entidad de pensión
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

        // 6. Notificación por correo electrónico al apoderado
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

        return mapToResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompromisoApoderadoResponse> listarCompromisosPorEstudianteYAnio(Long apoderadoId, Long estudianteId, Integer anio) {
        if (!estudianteRepository.existsByIdAndApoderadoId(estudianteId, apoderadoId)) {
            throw new ReglaNegocioException("El estudiante especificado no se encuentra asociado al apoderado autenticado.");
        }

        LocalDate hoy = LocalDate.now();
        return compromisoPagoRepository.findByEstudianteAndAnioOpcional(estudianteId, anio)
                .stream()
                .map(c -> mapToCompromisoApoderadoResponse(c, hoy))
                .toList();
    }

    // --- Métodos Auxiliares ---

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

        // Si el compromiso es de tipo Pension, este NUNCA se cancela

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