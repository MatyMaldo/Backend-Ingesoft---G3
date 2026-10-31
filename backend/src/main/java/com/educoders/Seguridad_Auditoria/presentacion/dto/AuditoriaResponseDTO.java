package com.educoders.Seguridad_Auditoria.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;

import java.time.LocalDateTime;
import java.util.List;

public class AuditoriaResponseDTO {
    private Long id;
    private LocalDateTime fechaHora;
    private Long usuarioId;
    private CategoriaAuditoria categoria;
    private String accion;
    private String entidad;
    private Long entidadId;
    private String descripcion;
    private List<DetalleAuditoriaDTO> detalles;

    public AuditoriaResponseDTO() {
    }

    public AuditoriaResponseDTO(Long id, LocalDateTime fechaHora, Long usuarioId,
                                CategoriaAuditoria categoria, String accion,
                                String entidad, Long entidadId,
                                String descripcion, List<DetalleAuditoriaDTO> detalles) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.usuarioId = usuarioId;
        this.categoria = categoria;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.descripcion = descripcion;
        this.detalles = detalles;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public CategoriaAuditoria getCategoria() { return categoria; }
    public void setCategoria(CategoriaAuditoria categoria) { this.categoria = categoria; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public Long getEntidadId() { return entidadId; }
    public void setEntidadId(Long entidadId) { this.entidadId = entidadId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public List<DetalleAuditoriaDTO> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleAuditoriaDTO> detalles) { this.detalles = detalles; }
}