package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.presentacion.dto.AuditoriaResponseDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaService {
    void registrarAuditoria(RegistrarAuditoriaRequestDTO dto);
    Page<AuditoriaResponseDTO> listarAuditorias(TipoAccionAuditoria accion,
                                                LocalDateTime fechaInicio, LocalDateTime fechaFin, Pageable pageable);
    List<AuditoriaResponseDTO> obtenerHistorialPorEntidad(String entidad, Long entidadId);
    AuditoriaResponseDTO obtenerPorId(Long id);
}