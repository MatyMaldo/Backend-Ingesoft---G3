package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.servicio.ServicioNotificacionEmail;
import com.educoders.Seguridad_Auditoria.dominio.TokenRestablecimiento;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.TokenRestablecimientoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class RestablecimientoPasswordServiceImpl implements RestablecimientoPasswordService {

    private static final Logger log = LoggerFactory.getLogger(RestablecimientoPasswordServiceImpl.class);
    private static final String MENSAJE_CODIGO_INVALIDO =
            "El código de verificación es inválido o ha expirado.";

    private final TokenRestablecimientoRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServicioNotificacionEmail servicioNotificacionEmail;
    private final AuditoriaService auditoriaService;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.password-reset.expiracion-minutos:15}")
    private long expiracionMinutos;

    @Value("${app.password-reset.cooldown-segundos:60}")
    private long cooldownSegundos;

    @Value("${app.password-reset.max-intentos:5}")
    private int maxIntentos;

    public RestablecimientoPasswordServiceImpl(
            TokenRestablecimientoRepository tokenRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            ServicioNotificacionEmail servicioNotificacionEmail,
            AuditoriaService auditoriaService) {
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.servicioNotificacionEmail = servicioNotificacionEmail;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public void solicitarCodigo(String email) {
        Usuario usuario = buscarUsuarioActivo(email);
        if (usuario == null) {
            // Respuesta genérica: no se revela si el correo existe.
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        Optional<TokenRestablecimiento> ultimo =
                tokenRepository.findTopByUsuarioIdOrderByFechaGeneracionDesc(usuario.getId());
        boolean enCooldown = ultimo
                .map(t -> t.getFechaGeneracion() != null
                        && t.getFechaGeneracion().plusSeconds(cooldownSegundos).isAfter(ahora))
                .orElse(false);
        if (enCooldown) {
            return;
        }

        List<TokenRestablecimiento> activos =
                tokenRepository.findByUsuarioIdAndUsadoFalse(usuario.getId());
        activos.forEach(t -> t.setUsado(true));
        if (!activos.isEmpty()) {
            tokenRepository.saveAll(activos);
        }

        String codigo = generarCodigo();
        TokenRestablecimiento token = new TokenRestablecimiento();
        token.setCodigoHash(passwordEncoder.encode(codigo));
        token.setFechaGeneracion(ahora);
        token.setFechaExpiracion(ahora.plusMinutes(expiracionMinutos));
        token.setUsado(false);
        token.setIntentos(0);
        token.setUsuario(usuario);
        tokenRepository.save(token);

        servicioNotificacionEmail.enviarCorreoRestablecimientoPassword(
                usuario.getEmail(),
                nombreCompleto(usuario),
                codigo,
                token.getFechaExpiracion());
    }

    @Override
    @Transactional
    public boolean verificarCodigo(String email, String codigo) {
        Usuario usuario = buscarUsuarioActivo(email);
        if (usuario == null) {
            return false;
        }
        try {
            validarCodigo(usuario, codigo);
            return true;
        } catch (ReglaNegocioException ex) {
            return false;
        }
    }

    @Override
    @Transactional(noRollbackFor = ReglaNegocioException.class)
    public void restablecerPassword(String email, String codigo, String nuevaPassword) {
        Usuario usuario = buscarUsuarioActivo(email);
        if (usuario == null) {
            throw new ReglaNegocioException(MENSAJE_CODIGO_INVALIDO);
        }
        if (nuevaPassword == null || nuevaPassword.isBlank()) {
            throw new ReglaNegocioException("La nueva contraseña es obligatoria.");
        }

        TokenRestablecimiento token = validarCodigo(usuario, codigo);

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        token.setUsado(true);
        tokenRepository.save(token);

        List<TokenRestablecimiento> otros =
                tokenRepository.findByUsuarioIdAndUsadoFalse(usuario.getId());
        otros.forEach(t -> t.setUsado(true));
        if (!otros.isEmpty()) {
            tokenRepository.saveAll(otros);
        }

        auditarRestablecimiento(usuario);
    }

    /**
     * Valida el código activo más reciente. Si es incorrecto incrementa el
     * contador de intentos; si expira o supera el máximo, lo marca como usado.
     */
    private TokenRestablecimiento validarCodigo(Usuario usuario, String codigo) {
        TokenRestablecimiento token = tokenRepository
                .findTopByUsuarioIdAndUsadoFalseOrderByFechaGeneracionDesc(usuario.getId())
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_CODIGO_INVALIDO));

        LocalDateTime ahora = LocalDateTime.now();
        if (token.getFechaExpiracion() == null
                || !token.getFechaExpiracion().isAfter(ahora)
                || token.getIntentos() >= maxIntentos) {
            token.setUsado(true);
            tokenRepository.save(token);
            throw new ReglaNegocioException(MENSAJE_CODIGO_INVALIDO);
        }

        if (!coincideCodigo(codigo, token.getCodigoHash())) {
            token.setIntentos((short) (token.getIntentos() + 1));
            if (token.getIntentos() >= maxIntentos) {
                token.setUsado(true);
            }
            tokenRepository.save(token);
            throw new ReglaNegocioException(MENSAJE_CODIGO_INVALIDO);
        }

        return token;
    }

    private boolean coincideCodigo(String codigo, String hash) {
        if (codigo == null || hash == null) {
            return false;
        }
        try {
            return passwordEncoder.matches(codigo, hash);
        } catch (IllegalArgumentException ex) {
            // Registros antiguos con un formato de hash no reconocido se tratan como inválidos.
            return false;
        }
    }

    private Usuario buscarUsuarioActivo(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return usuarioRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(Usuario::isActivo)
                .orElse(null);
    }

    private String generarCodigo() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() + " " + usuario.getApellidoPaterno();
        if (usuario.getApellidoMaterno() != null && !usuario.getApellidoMaterno().isBlank()) {
            nombre += " " + usuario.getApellidoMaterno();
        }
        return nombre;
    }

    private void auditarRestablecimiento(Usuario usuario) {
        try {
            auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                    usuario.getId(),
                    CategoriaAuditoria.SEGURIDAD,
                    TipoAccionAuditoria.ACTUALIZAR,
                    "Usuario",
                    usuario.getId(),
                    "Restablecimiento de contraseña",
                    List.of()
            ));
        } catch (RuntimeException ex) {
            log.warn("No se pudo auditar el restablecimiento de contraseña: {}", ex.getMessage());
        }
    }
}
