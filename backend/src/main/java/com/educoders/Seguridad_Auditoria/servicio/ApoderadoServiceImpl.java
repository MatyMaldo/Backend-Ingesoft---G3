package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.ApoderadoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.EstudianteApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarApoderadoRequest;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reglas de negocio de HU-GEMAP-01.
 * Responsabilidad única: alta de apoderados.
 */
@Service
public class ApoderadoServiceImpl implements ApoderadoService {

    private final UsuarioRepository usuarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final PasswordEncoder passwordEncoder;

    public ApoderadoServiceImpl(
            UsuarioRepository usuarioRepository,
            ApoderadoRepository apoderadoRepository,
            EstudianteRepository estudianteRepository,
            MatriculaRepository matriculaRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.estudianteRepository = estudianteRepository;
        this.matriculaRepository = matriculaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un apoderado y crea su cuenta de usuario.
     */
    @Override
    @Transactional
    public ApoderadoResponse registrarApoderado(RegistrarApoderadoRequest solicitud) {

        validarUnicidad(solicitud.dni(), solicitud.email());

        Apoderado apoderado = new Apoderado();

        apoderado.setNombre(solicitud.nombre());
        apoderado.setApellidoPaterno(solicitud.apellidoPaterno());
        apoderado.setApellidoMaterno(solicitud.apellidoMaterno());
        apoderado.setDni(solicitud.dni());

        // Username es la parte local del email (antes del @)
        apoderado.setUsername(extraerUsernameDeEmail(solicitud.email()));

        apoderado.setEmail(solicitud.email());
        apoderado.setTelefono(solicitud.telefono());

        // La contraseña no se guarda directamente.
        apoderado.setPasswordHash(
                passwordEncoder.encode(solicitud.password())
        );

        apoderado.setEdad(solicitud.edad());
        apoderado.setActivo(true);
        apoderado.setFechaCreacion(LocalDateTime.now());

        Apoderado apoderadoGuardado =
                apoderadoRepository.save(apoderado);

        return convertirARespuesta(apoderadoGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstudianteApoderadoResponse> listarMisEstudiantes(Long anioId) {
        Apoderado apoderado = exigirApoderado();

        return estudianteRepository.findByApoderadoIdOrderByIdAsc(apoderado.getId())
                .stream()
                .map(estudiante -> convertirARespuesta(estudiante, anioId))
                .toList();
    }

    private EstudianteApoderadoResponse convertirARespuesta(Estudiante estudiante, Long anioId) {
        Matricula matricula = null;
        if (anioId != null) {
            matricula = matriculaRepository.buscarPorEstudianteYAnioIdYEstados(
                            estudiante.getId(),
                            anioId,
                            List.of(EstadoMatricula.PENDIENTE, EstadoMatricula.CONFIRMADA))
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        String nombreCompleto = String.join(
                " ",
                estudiante.getNombre(),
                estudiante.getApellidoPaterno(),
                estudiante.getApellidoMaterno() == null ? "" : estudiante.getApellidoMaterno()
        ).trim();

        if (matricula == null) {
            return new EstudianteApoderadoResponse(
                    estudiante.getId(),
                    estudiante.getNombre(),
                    estudiante.getApellidoPaterno(),
                    estudiante.getApellidoMaterno(),
                    nombreCompleto,
                    estudiante.getDni(),
                    estudiante.getCodigo(),
                    null, null, null, null, null, null, null
            );
        }

        return new EstudianteApoderadoResponse(
                estudiante.getId(),
                estudiante.getNombre(),
                estudiante.getApellidoPaterno(),
                estudiante.getApellidoMaterno(),
                nombreCompleto,
                estudiante.getDni(),
                estudiante.getCodigo(),
                matricula.getSeccion().getGrado().getId(),
                matricula.getSeccion().getGrado().getNombre(),
                matricula.getSeccion().getId(),
                matricula.getSeccion().getNombre(),
                matricula.getSeccion().getAnioAcademico().getId(),
                matricula.getSeccion().getAnioAcademico().getAnio(),
                matricula.getEstado()
        );
    }

    private Apoderado exigirApoderado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        if (!(usuario instanceof Apoderado apoderado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un apoderado puede consultar sus estudiantes.");
        }
        return apoderado;
    }

    private ApoderadoResponse convertirARespuesta(Apoderado apoderado) {

        String nombreCompleto = String.join(
                " ",
                apoderado.getNombre(),
                apoderado.getApellidoPaterno(),
                apoderado.getApellidoMaterno() == null
                        ? ""
                        : apoderado.getApellidoMaterno()
        ).trim();

        return new ApoderadoResponse(
                apoderado.getId(),
                apoderado.getNombre(),
                apoderado.getApellidoPaterno(),
                apoderado.getApellidoMaterno(),
                nombreCompleto,
                apoderado.getDni(),
                apoderado.getUsername(),
                apoderado.getEmail(),
                apoderado.getTelefono(),
                apoderado.getEdad(),
                apoderado.isActivo(),
                apoderado.getFechaCreacion()
        );
    }

    private void validarUnicidad(String dni, String email) {

        if (usuarioRepository.existsByDniAndTipoUsuario(dni, Apoderado.class)) {
            throw new ConflictoException(
                    "Ya existe un apoderado registrado con el DNI indicado."
            );
        }

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException(
                    "Ya existe un usuario registrado con el correo indicado."
            );
        }

        if (usuarioRepository.existsByUsername(extraerUsernameDeEmail(email))) {
            throw new ConflictoException(
                    "Ya existe un usuario registrado con ese identificador."
            );
        }
    }

    private String extraerUsernameDeEmail(String email) {
        return email.substring(0, email.indexOf('@'));
    }
}