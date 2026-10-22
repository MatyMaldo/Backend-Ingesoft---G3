package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.CompromisoPagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoPagoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoMatriculaRequest;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CompromisoPagoServiceImpl implements CompromisoPagoService {

    private static final BigDecimal MONTO_MATRICULA_PREDETERMINADO = new BigDecimal("350.00");

    private final CompromisoPagoRepository compromisoPagoRepository;
    private final MatriculaRepository matriculaRepository;
    private final PeriodoMatriculaRepository periodoMatriculaRepository;

    public CompromisoPagoServiceImpl(CompromisoPagoRepository compromisoPagoRepository,
                                     MatriculaRepository matriculaRepository,
                                     PeriodoMatriculaRepository periodoMatriculaRepository) {
        this.compromisoPagoRepository = compromisoPagoRepository;
        this.matriculaRepository = matriculaRepository;
        this.periodoMatriculaRepository = periodoMatriculaRepository;
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

        // 5. Asignar monto original
        BigDecimal montoFinal = (request.montoPersonalizado() != null && request.montoPersonalizado().compareTo(BigDecimal.ZERO) > 0)
                ? request.montoPersonalizado()
                : MONTO_MATRICULA_PREDETERMINADO;

        // 6. Construir y guardar entidad
        CompromisoPago compromiso = CompromisoPago.builder()
                .tipoCompromiso(TipoCompromiso.MATRICULA)
                .mes(null) // Para matrículas mes es null por BD
                .fechaEmision(hoy)
                .montoOriginal(montoFinal)
                .montoMora(BigDecimal.ZERO)
                .estado(EstadoCompromisoPago.PENDIENTE)
                .matricula(matricula)
                .build();

        CompromisoPago guardado = compromisoPagoRepository.save(compromiso);

        return mapToResponse(guardado);
    }

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