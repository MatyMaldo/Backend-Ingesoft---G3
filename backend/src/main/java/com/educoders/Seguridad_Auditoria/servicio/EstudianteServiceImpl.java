package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.persistencia.ApoderadoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.EstudianteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarEstudianteRequest;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio para el registro de estudiantes.
 */
@Service
public class EstudianteServiceImpl implements EstudianteService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final PasswordEncoder passwordEncoder;

    public EstudianteServiceImpl(
            UsuarioRepository usuarioRepository,
            EstudianteRepository estudianteRepository,
            ApoderadoRepository apoderadoRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public EstudianteResponse registrarEstudiante(
            RegistrarEstudianteRequest solicitud) {

        validarUnicidad(
                solicitud.dni(),
                solicitud.email(),
                solicitud.codigo()
        );

        Apoderado apoderado = apoderadoRepository
                .findById(solicitud.apoderadoId())
                .orElseThrow(() ->
                        RecursoNoEncontradoException.de(
                                "el apoderado",
                                solicitud.apoderadoId()
                        )
                );

        Estudiante estudiante = new Estudiante();

        estudiante.setNombre(solicitud.nombre());
        estudiante.setApellidoPaterno(solicitud.apellidoPaterno());
        estudiante.setApellidoMaterno(solicitud.apellidoMaterno());
        estudiante.setDni(solicitud.dni());

        estudiante.setUsername(solicitud.dni());

        estudiante.setEmail(solicitud.email());
        estudiante.setTelefono(solicitud.telefono());

        estudiante.setPasswordHash(
                passwordEncoder.encode(solicitud.password())
        );

        estudiante.setCodigo(solicitud.codigo());
        estudiante.setFechaNacimiento(solicitud.fechaNacimiento());
        estudiante.setApoderado(apoderado);

        estudiante.setActivo(true);
        estudiante.setFechaCreacion(LocalDateTime.now());

        Estudiante estudianteGuardado =
                estudianteRepository.save(estudiante);

        return convertirARespuesta(estudianteGuardado);
    }

    private EstudianteResponse convertirARespuesta(
            Estudiante estudiante) {

        String nombreCompleto = String.join(
                " ",
                estudiante.getNombre(),
                estudiante.getApellidoPaterno(),
                estudiante.getApellidoMaterno() == null
                        ? ""
                        : estudiante.getApellidoMaterno()
        ).trim();

        return new EstudianteResponse(
                estudiante.getId(),
                estudiante.getNombre(),
                estudiante.getApellidoPaterno(),
                estudiante.getApellidoMaterno(),
                nombreCompleto,
                estudiante.getDni(),
                estudiante.getUsername(),
                estudiante.getEmail(),
                estudiante.getTelefono(),
                estudiante.getCodigo(),
                estudiante.getFechaNacimiento(),
                estudiante.getApoderado().getId(),
                estudiante.isActivo(),
                estudiante.getFechaCreacion()
        );
    }

    private void validarUnicidad(
            String dni,
            String email,
            String codigo) {

        if (usuarioRepository.existsByDniAndTipoUsuario(dni, Estudiante.class)) {
            throw new ConflictoException(
                    "Ya existe un estudiante registrado con el DNI indicado."
            );
        }

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException(
                    "Ya existe un usuario registrado con el correo indicado."
            );
        }

        if (usuarioRepository.existsByUsername(dni)) {
            throw new ConflictoException(
                    "Ya existe un usuario registrado con ese identificador."
            );
        }

        if (estudianteRepository.existsByCodigo(codigo)) {
            throw new ConflictoException(
                    "Ya existe un estudiante registrado con ese código."
            );
        }
    }
}