package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.presentacion.dto.AuditoriaResponseDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaService {
    /**
     * Registra una auditoría dentro de la transacción de la operación auditada (HU-SAT-04, atomicidad).
     * Debe llamarse con una transacción abierta; si no puede guardarse lanza una excepción, que cancela la operación.
     */
    void registrarAuditoria(RegistrarAuditoriaRequestDTO dto);
    Page<AuditoriaResponseDTO> listarAuditorias(CategoriaAuditoria categoria,
                                                TipoAccionAuditoria accion,
                                                LocalDateTime fechaInicio, LocalDateTime fechaFin,
                                                String busqueda, Pageable pageable);
    Page<AuditoriaResponseDTO> obtenerHistorialPorEntidad(String entidad, Long entidadId, Pageable pageable);
    AuditoriaResponseDTO obtenerPorId(Long id);
}