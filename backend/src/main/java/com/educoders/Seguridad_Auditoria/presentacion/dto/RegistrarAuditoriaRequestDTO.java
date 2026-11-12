package com.educoders.Seguridad_Auditoria.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;

import java.util.ArrayList;
import java.util.List;

public class RegistrarAuditoriaRequestDTO {
    private Long usuarioId;
    private CategoriaAuditoria categoria;
    private TipoAccionAuditoria accion;
    private String entidad;
    private Long entidadId;
    private String descripcion;
    private List<DetalleAuditoriaDTO> detalles = new ArrayList<>();
    /**
     * Si es {@code true}, {@link #usuarioId} es el autor definitivo (incluso {@code null}) y NO se sustituye por el
     * usuario del token. Se usa en los eventos de sesión: quien inicia sesión o falla al hacerlo no es el usuario que
     * pudiera venir en un token anterior.
     */
    private boolean autorFijo;

    public RegistrarAuditoriaRequestDTO() {
    }

    public RegistrarAuditoriaRequestDTO(Long usuarioId, CategoriaAuditoria categoria,
                                        TipoAccionAuditoria accion, String entidad,
                                        Long entidadId, String descripcion, List<DetalleAuditoriaDTO> detalles) {
        this.usuarioId = usuarioId;
        this.categoria = categoria;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.descripcion = descripcion;
        this.detalles = detalles != null ? detalles : new ArrayList<>();
    }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public CategoriaAuditoria getCategoria() { return categoria; }
    public void setCategoria(CategoriaAuditoria categoria) { this.categoria = categoria; }

    public TipoAccionAuditoria getAccion() { return accion; }
    public void setAccion(TipoAccionAuditoria accion) { this.accion = accion; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public Long getEntidadId() { return entidadId; }
    public void setEntidadId(Long entidadId) { this.entidadId = entidadId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isAutorFijo() { return autorFijo; }
    public void setAutorFijo(boolean autorFijo) { this.autorFijo = autorFijo; }

    /** Marca el autor como definitivo (ver {@link #autorFijo}) y devuelve el mismo DTO. */
    public RegistrarAuditoriaRequestDTO conAutorFijo() {
        this.autorFijo = true;
        return this;
    }

    public List<DetalleAuditoriaDTO> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleAuditoriaDTO> detalles) { this.detalles = detalles; }
}