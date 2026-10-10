package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.persistencia.ApoderadoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarApoderadoRequest;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de HU-GEMAP-01.
 * Responsabilidad única: alta de apoderados.
 */
@Service
public class ApoderadoServiceImpl implements ApoderadoService {

    private final UsuarioRepository usuarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final PasswordEncoder passwordEncoder;

    public ApoderadoServiceImpl(
            UsuarioRepository usuarioRepository,
            ApoderadoRepository apoderadoRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.apoderadoRepository = apoderadoRepository;
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

        // Igual que en el registro de docentes: el DNI será el username.
        apoderado.setUsername(solicitud.dni());

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

        if (usuarioRepository.existsByDni(dni)) {
            throw new ConflictoException(
                    "Ya existe un usuario registrado con el DNI indicado."
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
    }
}