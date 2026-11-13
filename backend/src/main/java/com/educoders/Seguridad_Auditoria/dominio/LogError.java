package com.educoders.Seguridad_Auditoria.dominio;

import com.educoders.Infraestructura_Servicios.enums.Modulo;
import com.educoders.Infraestructura_Servicios.enums.TipoError;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Error registrado por el sistema (HU-SAT-06). Una vez creado es de solo lectura: nadie puede modificarlo ni
 * eliminarlo, ni siquiera el administrador (Esc. 5). {@link Immutable} hace que Hibernate ignore cualquier cambio y los
 * callbacks de ciclo de vida lo rechazan explícitamente; en la base lo refuerzan los triggers de {@code logs_error}.
 */
@Entity
@Immutable
@Table(name = "logs_error")
public class LogError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "modulo", nullable = false, length = 60)
    private Modulo modulo;

    @Convert(converter = TipoErrorConverter.class)
    @Column(name = "tipo_error", nullable = false, length = 160)
    private TipoError tipoError;

    @Column(nullable = false, length = 2000)
    private String mensaje;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column(length = 500)
    private String endpoint;

    @Column(name = "metodo_http", length = 10)
    private String metodoHttp;

    @Column(name = "codigo_estado")
    private Short codigoEstado;

    public LogError() {}

    @PreUpdate
    void rechazarModificacion() {
        throw new IllegalStateException("Los registros del log de errores no pueden modificarse.");
    }

    @PreRemove
    void rechazarEliminacion() {
        throw new IllegalStateException("Los registros del log de errores no pueden eliminarse.");
    }

    public LogError(LocalDateTime fechaHora, Usuario usuario, Modulo modulo, TipoError tipoError,
                    String mensaje, String detalle, String endpoint, String metodoHttp, Short codigoEstado) {
        this.fechaHora = fechaHora;
        this.usuario = usuario;
        this.modulo = modulo;
        this.tipoError = tipoError;
        this.mensaje = mensaje;
        this.detalle = detalle;
        this.endpoint = endpoint;
        this.metodoHttp = metodoHttp;
        this.codigoEstado = codigoEstado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Modulo getModulo() { return modulo; }
    public void setModulo(Modulo modulo) { this.modulo = modulo; }

    public TipoError getTipoError() { return tipoError; }
    public void setTipoError(TipoError tipoError) { this.tipoError = tipoError; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getMetodoHttp() { return metodoHttp; }
    public void setMetodoHttp(String metodoHttp) { this.metodoHttp = metodoHttp; }

    public Short getCodigoEstado() { return codigoEstado; }
    public void setCodigoEstado(Short codigoEstado) { this.codigoEstado = codigoEstado; }
}