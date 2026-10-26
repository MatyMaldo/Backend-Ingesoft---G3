package com.educoders.Seguridad_Auditoria.dominio;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
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

    @Column(name = "tipo_error", nullable = false, length = 160)
    private String tipoError;

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

    public LogError(LocalDateTime fechaHora, Usuario usuario, String tipoError,
                    String mensaje, String detalle, String endpoint, String metodoHttp, Short codigoEstado) {
        this.fechaHora = fechaHora;
        this.usuario = usuario;
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

    public String getTipoError() { return tipoError; }
    public void setTipoError(String tipoError) { this.tipoError = tipoError; }

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