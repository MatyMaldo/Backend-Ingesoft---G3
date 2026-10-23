package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Pago;
import com.educoders.Gestion_Matricula_Pagos.persistencia.CompromisoPagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PagoRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.PagoApoderadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarPagoRequest;
import com.educoders.Infraestructura_Servicios.enums.EstadoPago;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.servicio.ServicioAlmacenamientoArchivo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final CompromisoPagoRepository compromisoPagoRepository;
    private final ServicioAlmacenamientoArchivo servicioAlmacenamientoArchivo;

    public PagoServiceImpl(PagoRepository pagoRepository,
                           CompromisoPagoRepository compromisoPagoRepository, ServicioAlmacenamientoArchivo servicioAlmacenamientoArchivo) {
        this.pagoRepository = pagoRepository;
        this.compromisoPagoRepository = compromisoPagoRepository;
        this.servicioAlmacenamientoArchivo = servicioAlmacenamientoArchivo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoApoderadoResponse> listarPagosPorCompromisoYApoderado(Long apoderadoId, Long compromisoId) {
        CompromisoPago compromiso = compromisoPagoRepository.findById(compromisoId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Compromiso de Pago", compromisoId));

        // Regla de seguridad: Garantizar que el compromiso pertenezca a un estudiante a cargo de este apoderado
        Long apoderadoAsociadoId = compromiso.getMatricula().getEstudiante().getApoderado().getId();
        if (!apoderadoAsociadoId.equals(apoderadoId)) {
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
        // 1. Validar existencia del compromiso
        CompromisoPago compromiso = compromisoPagoRepository.findById(request.compromisoId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Compromiso de Pago", request.compromisoId()));

        // 2. Validar que el compromiso pertenezca a un estudiante a cargo del apoderado solicitante
        var apoderado = compromiso.getMatricula().getEstudiante().getApoderado();
        if (!apoderado.getId().equals(request.apoderadoId())) {
            throw new ReglaNegocioException("El compromiso de pago no pertenece a ningún estudiante a su cargo.");
        }

        // 3. Validar que no exista un pago en revisión (PENDIENTE) para el mismo compromiso
        if (pagoRepository.existsByCompromisoPagoIdAndEstado(request.compromisoId(), EstadoPago.PENDIENTE)) {
            throw new ReglaNegocioException("Ya existe una solicitud de pago pendiente de validación para este compromiso.");
        }

        // 4. Guardar archivo en Cloudinary (aplica validaciones de PDF y max 5MB)
        String evidenciaUrl = servicioAlmacenamientoArchivo.guardarArchivo(request.comprobante(), "comprobantes/pagos");

        // 5. Instanciar mediante el Builder y persistir
        Pago pago = Pago.builder()
                .fechaRegistro(LocalDate.now())
                .montoPagado(request.montoPagado())
                .evidenciaUrl(evidenciaUrl)
                .estado(EstadoPago.PENDIENTE)
                .compromisoPago(compromiso)
                .apoderadoPagador(apoderado)
                .build();

        Pago pagoGuardado = pagoRepository.save(pago);

        // 6. Mapear y retornar la respuesta
        return mapToPagoApoderadoResponse(pagoGuardado);
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