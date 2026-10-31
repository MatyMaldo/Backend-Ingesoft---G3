package com.educoders.Seguridad_Auditoria.dominio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auditorias")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 60)
    private CategoriaAuditoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 60)
    private TipoAccionAuditoria accion;

    @Column(name = "entidad", nullable = false, length = 100)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @OneToMany(mappedBy = "auditoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleAuditoria> detalles = new ArrayList<>();

    public Auditoria() {
    }

    public void agregarDetalle(DetalleAuditoria detalle) {
        detalles.add(detalle);
        detalle.setAuditoria(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

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

    public List<DetalleAuditoria> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleAuditoria> detalles) { this.detalles = detalles; }
}