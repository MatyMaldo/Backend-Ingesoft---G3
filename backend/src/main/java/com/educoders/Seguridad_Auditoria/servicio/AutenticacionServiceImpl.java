
package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AutenticacionServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponse iniciarSesion(LoginRequest request) {

        String identificador = request.identificador().trim();

        // Primera iteración: solo Administrador.
        if (!"ADMINISTRADOR".equalsIgnoreCase(request.perfil().trim())) {
            throw credencialesInvalidas();
        }

        // Buscar por correo electrónico o DNI.
        Usuario usuario = (identificador.contains("@")
                ? usuarioRepository.findByEmail(identificador)
                : usuarioRepository.findByDni(identificador))
                .orElseThrow(this::credencialesInvalidas);

        // Comprobar el perfil real de la cuenta.
        if (!(usuario instanceof Administrador)) {
            throw credencialesInvalidas();
        }

        // Verificar la contraseña con BCrypt.
        if (!passwordEncoder.matches(
                request.password(),
                usuario.getPasswordHash()
        )) {
            throw credencialesInvalidas();
        }

        // Comprobar el estado de la cuenta.
        if (!usuario.isActivo()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tu cuenta está inactiva. Comunícate con el administrador"
            );
        }

        // Generar sesión JWT.
        String token = jwtService.generarToken(usuario, "ADMINISTRADOR");

        LoginResponse.UsuarioLoginResponse datosUsuario =
                new LoginResponse.UsuarioLoginResponse(
                        usuario.getId(),
                        usuario.getNombre(),
                        "ADMINISTRADOR"
                );

        return new LoginResponse(token, datosUsuario);
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Correo/DNI o contraseña incorrectos"
        );
    }
}
