
package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
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
    private final DocenteRepository docenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AutenticacionServiceImpl(
            UsuarioRepository usuarioRepository,
            DocenteRepository docenteRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponse iniciarSesion(LoginRequest request) {

        String identificador = request.identificador().trim();
        String perfil = request.perfil().trim().toUpperCase();
        Usuario usuario;

        //Sprint 2: Login de Administrador y Docente
        if ("ADMINISTRADOR".equals(perfil)) {
            usuario = (identificador.contains("@")
                    ? usuarioRepository.findByEmail(identificador)
                    : usuarioRepository.findByDni(identificador))
                    .orElseThrow(this::credencialesInvalidas);
            if (!(usuario instanceof Administrador)) throw credencialesInvalidas();
        } else if ("DOCENTE".equals(perfil)) {
            usuario = (identificador.contains("@")
                    ? docenteRepository.findByEmail(identificador)
                    : docenteRepository.findByDni(identificador))
                    .orElseThrow(this::credencialesInvalidas);
            if (!(usuario instanceof Docente)) throw credencialesInvalidas();
        } else {
            throw credencialesInvalidas();
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) throw credencialesInvalidas();
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está inactiva. Comunícate con el administrador");

        String token = jwtService.generarToken(usuario, perfil);
        LoginResponse.UsuarioLoginResponse datosUsuario = new LoginResponse.UsuarioLoginResponse(usuario.getId(), usuario.getNombre(), perfil);
        return new LoginResponse(token, datosUsuario);
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Correo/DNI o contraseña incorrectos"
        );
    }
}
