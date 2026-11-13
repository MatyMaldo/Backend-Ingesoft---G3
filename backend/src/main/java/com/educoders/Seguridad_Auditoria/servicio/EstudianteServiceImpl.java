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
    private final AuditoriaService auditoriaService;

    public EstudianteServiceImpl(
            UsuarioRepository usuarioRepository,
            EstudianteRepository estudianteRepository,
            ApoderadoRepository apoderadoRepository,
            AuditoriaService auditoriaService) {

        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.auditoriaService = auditoriaService;
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

        Estudiante estudianteGuardado =
                estudianteRepository.save(construirEstudiante(solicitud, apoderado));
        // HU-SAT-04: el alta y su registro de auditoría se confirman juntos (misma transacción).
        auditoriaService.registrarAuditoria(AuditoriaUsuarios.registro(estudianteGuardado));

        return convertirARespuesta(estudianteGuardado);
    }

    @Override
    @Transactional
    public void registrarEstudianteEnLote(
            RegistrarEstudianteRequest solicitud,
            Apoderado apoderado) {

        estudianteRepository.save(construirEstudiante(solicitud, apoderado));
    }

    private Estudiante construirEstudiante(
            RegistrarEstudianteRequest solicitud,
            Apoderado apoderado) {

        Estudiante estudiante = new Estudiante();

        estudiante.setNombre(solicitud.nombre());
        estudiante.setApellidoPaterno(solicitud.apellidoPaterno());
        estudiante.setApellidoMaterno(solicitud.apellidoMaterno());
        estudiante.setDni(solicitud.dni());

        // Username es la parte local del email (antes del @)
        estudiante.setUsername(extraerUsernameDeEmail(solicitud.email()));

        estudiante.setEmail(solicitud.email());
        estudiante.setTelefono(solicitud.telefono());

        // La cuenta nace SIN contraseña (passwordHash nulo): el administrador no la conoce ni la define.
        // TODO(HU-SAT-03, 2026-11-08): al crear la cuenta, solicitar el envío del código de verificación al correo
        // para que el usuario defina su contraseña. Pendiente del servicio de restablecimiento (otro integrante);
        // debe ejecutarse de forma asíncrona y su falla no debe revertir el registro.

        estudiante.setCodigo(solicitud.codigo());
        estudiante.setFechaNacimiento(solicitud.fechaNacimiento());
        estudiante.setApoderado(apoderado);

        estudiante.setActivo(true);
        estudiante.setFechaCreacion(LocalDateTime.now());

        return estudiante;
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

        if (usuarioRepository.existsByUsername(extraerUsernameDeEmail(email))) {
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

    private String extraerUsernameDeEmail(String email) {
        return email.substring(0, email.indexOf('@'));
    }
}