
package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;

import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LoginResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

        Usuario usuario;

        if (identificador.contains("@")) {
            usuario = usuarioRepository.findByEmail(identificador)
                    .orElseThrow(this::credencialesInvalidas);

            if (!contrasenaCorrecta(usuario, request.password())) {
                throw credencialesInvalidas();
            }

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
                throw credencialesInvalidas();
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
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tu cuenta está inactiva. Comunícate con el administrador"
            );
        }

        // Identificar automáticamente el perfil real.
        String perfil = obtenerPerfil(usuario);

        // Generar el JWT con el perfil identificado.
        String token = jwtService.generarToken(usuario, perfil);

        LoginResponse.UsuarioLoginResponse datosUsuario =
                new LoginResponse.UsuarioLoginResponse(
                        usuario.getId(),
                        usuario.getNombre(),
                        perfil
                );

        return new LoginResponse(token, datosUsuario);
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
