
package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.ApoderadoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
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
    private final EstudianteRepository estudianteRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AutenticacionServiceImpl(
            UsuarioRepository usuarioRepository,
            DocenteRepository docenteRepository,
            EstudianteRepository estudianteRepository,
            ApoderadoRepository apoderadoRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.estudianteRepository = estudianteRepository;
        this.apoderadoRepository = apoderadoRepository;
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
        } else if ("ESTUDIANTE".equals(perfil)) {
            usuario = (identificador.contains("@")
                    ? estudianteRepository.findByEmail(identificador)
                    : estudianteRepository.findByDni(identificador))
                    .orElseThrow(this::credencialesInvalidas);
            if (!(usuario instanceof Estudiante)) throw credencialesInvalidas();
        } else if ("APODERADO".equals(perfil)) {
            usuario = (identificador.contains("@")
                    ? apoderadoRepository.findByEmail(identificador)
                    : apoderadoRepository.findByDni(identificador))
                    .orElseThrow(this::credencialesInvalidas);
            if (!(usuario instanceof Apoderado)) throw credencialesInvalidas();
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
