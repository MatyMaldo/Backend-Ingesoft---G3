package com.educoders.Seguridad_Auditoria.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Código de verificación para restablecer o definir la contraseña de un
 * usuario. El código se guarda hasheado; nunca en texto plano.
 */
@Entity
@Table(name = "tokens_restablecimiento")
public class TokenRestablecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_hash", nullable = false, length = 255)
    private String codigoHash;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private boolean usado;

    @Column(nullable = false)
    private short intentos;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public TokenRestablecimiento() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoHash() { return codigoHash; }
    public void setCodigoHash(String codigoHash) { this.codigoHash = codigoHash; }

    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }

    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }

    public short getIntentos() { return intentos; }
    public void setIntentos(int intentos) { this.intentos = (short) intentos; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
