
package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;

import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionServiceImpl.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;
    private final TransactionTemplate transacciones;

    public AutenticacionServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditoriaService auditoriaService,
            PlatformTransactionManager transactionManager
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditoriaService = auditoriaService;
        this.transacciones = new TransactionTemplate(transactionManager);
    }

    /**
     * HU-SAT-07: inicia sesión y deja el inicio de sesión (o el intento fallido) en la auditoría de HU-SAT-04.
     *
     * <p>No es {@code @Transactional}: un intento fallido lanza una excepción y, dentro de una transacción de método,
     * revertiría el registro del propio intento. Cada registro se guarda en su transacción corta, y el inicio de
     * sesión exitoso es atómico con su auditoría: si esta no puede guardarse, el error se propaga y no se entrega
     * ningún token (no se crea la sesión).
     */
    @Override
    public LoginResponse iniciarSesion(LoginRequest request) {

        String identificador = request.identificador().trim();

        Usuario usuario;

        if (identificador.contains("@")) {
            Usuario encontrado = usuarioRepository.findByEmail(identificador).orElse(null);

            if (encontrado == null) {
                // El correo no corresponde a ninguna cuenta: autor "No determinado".
                throw intentoFallido(null);
            }
            if (!contrasenaCorrecta(encontrado, request.password())) {
                throw intentoFallido(encontrado);
            }
            usuario = encontrado;

        } else {
            List<Usuario> usuarios =
                    usuarioRepository.findAllByDni(identificador);

            // Buscar todas las cuentas cuya contraseña coincida.
            List<Usuario> coincidencias = usuarios.stream()
                    .filter(u -> contrasenaCorrecta(
                            u, request.password()
                    ))
                    .toList();

            if (coincidencias.isEmpty()) {
                // Con una sola cuenta para ese DNI se sabe a quién pertenece; con varias o ninguna, no.
                throw intentoFallido(usuarios.size() == 1 ? usuarios.get(0) : null);
            }

            // Evitar elegir arbitrariamente entre varias cuentas.
            if (coincidencias.size() > 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Existen varias cuentas asociadas a estas credenciales. " +
                                "Inicia sesión con tu correo electrónico."
                );
            }

            usuario = coincidencias.get(0);
        }

        // Impedir el acceso a cuentas inactivas.
        if (!usuario.isActivo()) {
            registrarIntentoFallido(usuario);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tu cuenta está inactiva. Comunícate con el administrador"
            );
        }

        // Identificar automáticamente el perfil real.
        String perfil = obtenerPerfil(usuario);

        // Generar el JWT con el perfil identificado.
        String token = jwtService.generarToken(usuario, perfil);

        // Atomicidad (HU-SAT-04 / HU-SAT-07 Esc. 12): si la auditoría falla, el error se propaga y no hay sesión.
        transacciones.executeWithoutResult(estado ->
                auditoriaService.registrarAuditoria(AuditoriaUsuarios.inicioDeSesion(usuario)));

        LoginResponse.UsuarioLoginResponse datosUsuario =
                new LoginResponse.UsuarioLoginResponse(
                        usuario.getId(),
                        usuario.getNombre(),
                        perfil
                );

        return new LoginResponse(token, datosUsuario);
    }

    /** HU-SAT-07 Esc. 7: registra el cierre de sesión voluntario del usuario autenticado. */
    @Override
    @Transactional
    public void cerrarSesion(Long usuarioId) {
        auditoriaService.registrarAuditoria(AuditoriaUsuarios.cierreDeSesion(usuarioId));
    }

    /**
     * Registra el intento fallido de {@code titular} (o "No determinado" si es {@code null}) y devuelve el error de
     * credenciales. Una falla al auditar el intento no debe cambiar la respuesta al usuario: solo se deja en el log.
     */
    private ResponseStatusException intentoFallido(Usuario titular) {
        registrarIntentoFallido(titular);
        return credencialesInvalidas();
    }

    private void registrarIntentoFallido(Usuario titular) {
        RegistrarAuditoriaRequestDTO auditoria = AuditoriaUsuarios.intentoFallido(titular);
        try {
            transacciones.executeWithoutResult(estado -> auditoriaService.registrarAuditoria(auditoria));
        } catch (RuntimeException ex) {
            log.error("No se pudo registrar el intento fallido de inicio de sesión: {}", ex.getMessage(), ex);
        }
    }

    private boolean contrasenaCorrecta(
            Usuario usuario,
            String password
    ) {
        return usuario.getPasswordHash() != null &&
                passwordEncoder.matches(
                        password,
                        usuario.getPasswordHash()
                );
    }

    private String obtenerPerfil(Usuario usuario) {

        if (usuario instanceof Administrador) {
            return "ADMINISTRADOR";
        }

        if (usuario instanceof Docente) {
            return "DOCENTE";
        }

        if (usuario instanceof Estudiante) {
            return "ESTUDIANTE";
        }

        if (usuario instanceof Apoderado) {
            return "APODERADO";
        }

        throw credencialesInvalidas();
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Correo/DNI o contraseña incorrectos"
        );
    }
}
