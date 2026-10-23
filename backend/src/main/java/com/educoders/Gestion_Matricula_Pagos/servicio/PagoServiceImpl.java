package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Pago;
import com.educoders.Gestion_Matricula_Pagos.persistencia.CompromisoPagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PagoRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.PagoApoderadoResponse;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final CompromisoPagoRepository compromisoPagoRepository;

    public PagoServiceImpl(PagoRepository pagoRepository,
                           CompromisoPagoRepository compromisoPagoRepository) {
        this.pagoRepository = pagoRepository;
        this.compromisoPagoRepository = compromisoPagoRepository;
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