package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Seguridad_Auditoria.dominio.Auditoria;
import com.educoders.Seguridad_Auditoria.dominio.DetalleAuditoria;
import com.educoders.Seguridad_Auditoria.persistencia.AuditoriaRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.AuditoriaResponseDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleAuditoriaDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaServiceImpl.class);
    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaServiceImpl(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAuditoria(RegistrarAuditoriaRequestDTO dto) {
        try {
            Long usuarioIdFinal = obtenerUsuarioIdAutenticado(dto.getUsuarioId());

            Auditoria auditoria = new Auditoria();
            auditoria.setFechaHora(LocalDateTime.now());
            auditoria.setUsuarioId(usuarioIdFinal);
            auditoria.setCategoria(dto.getCategoria() != null ? dto.getCategoria() : CategoriaAuditoria.SISTEMA);
            auditoria.setAccion(dto.getAccion());
            auditoria.setEntidad(dto.getEntidad());
            auditoria.setEntidadId(dto.getEntidadId());
            auditoria.setDescripcion(dto.getDescripcion());

            if (dto.getDetalles() != null) {
                for (DetalleAuditoriaDTO detalleDto : dto.getDetalles()) {
                    DetalleAuditoria detalle = new DetalleAuditoria(
                            detalleDto.getCampo(),
                            detalleDto.getValorAnterior(),
                            detalleDto.getValorNuevo()
                    );
                    auditoria.agregarDetalle(detalle);
                }
            }

            auditoriaRepository.saveAndFlush(auditoria);
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría para la entidad '{}': {}", dto.getEntidad(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> listarAuditorias(CategoriaAuditoria categoria,
                                                       TipoAccionAuditoria accion,
                                                       LocalDateTime fechaInicio, LocalDateTime fechaFin, Pageable pageable) {
        Page<Auditoria> auditorias = auditoriaRepository.buscarConFiltros(categoria, accion, fechaInicio, fechaFin, pageable);
        return auditorias.map(this::mapearAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> obtenerHistorialPorEntidad(String entidad, Long entidadId, Pageable pageable) {
        Page<Auditoria> historial = auditoriaRepository
                .findByEntidadAndEntidadIdOrderByFechaHoraAsc(entidad, entidadId, pageable);
        return historial.map(this::mapearAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriaResponseDTO obtenerPorId(Long id) {
        Auditoria auditoria = auditoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Auditoría no encontrada"));
        return mapearAResponse(auditoria);
    }

    private Long obtenerUsuarioIdAutenticado(Long idProporcionado) {
        // 1) Prioridad al usuario autenticado en el token (no confiable en el payload).
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                String principal = auth.getPrincipal().toString();
                if (!principal.isBlank() && !"anonymousUser".equalsIgnoreCase(principal)) {
                    try {
                        return Long.valueOf(principal);
                    } catch (NumberFormatException ignored) {
                        // principal no numérico; se ignora
                    }
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo extraer el usuario del SecurityContext: {}", e.getMessage());
        }
        // 2) Fallback: id proporcionado (procesos internos / sistema).
        return idProporcionado;
    }

    private AuditoriaResponseDTO mapearAResponse(Auditoria auditoria) {
        return new AuditoriaResponseDTO(
                auditoria.getId(),
                auditoria.getFechaHora(),
                auditoria.getUsuarioId(),
                auditoria.getCategoria(),
                auditoria.getAccion().name(),
                auditoria.getEntidad(),
                auditoria.getEntidadId(),
                auditoria.getDescripcion(),
                auditoria.getDetalles().stream()
                        .map(d -> new DetalleAuditoriaDTO(d.getCampo(), d.getValorAnterior(), d.getValorNuevo()))
                        .collect(Collectors.toList())
        );
    }
}