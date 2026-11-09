package com.educoders.Seguridad_Auditoria.servicio;


import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DocenteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarDocenteRequest;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de HU-SAT-01.
 * Responsabilidad única: alta de docentes.
 */
@Service
public class DocenteServiceImpl implements DocenteService {

    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;

    public DocenteServiceImpl(UsuarioRepository usuarioRepository,
                              DocenteRepository docenteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
    }

    /**
     * Regla de negocio: HU-SAT-01 — el administrador registra un docente
     * con datos básicos; el sistema crea la cuenta, le asigna el rol
     * DOCENTE y la habilita para iniciar sesión. El DNI y el correo
     * deben ser únicos en el sistema.
     */
    @Override
    @Transactional
    public DocenteResponse registrarDocente(RegistrarDocenteRequest solicitud) {
        validarUnicidad(solicitud.dni(), solicitud.email());

        Docente docente = new Docente();
        docente.setNombre(solicitud.nombre());
        docente.setApellidoPaterno(solicitud.apellidoPaterno());
        docente.setApellidoMaterno(solicitud.apellidoMaterno());
        docente.setDni(solicitud.dni());
        docente.setUsername(extraerUsernameDeEmail(solicitud.email()));
        docente.setEmail(solicitud.email());
        docente.setTelefono(solicitud.telefono());
        // La cuenta nace SIN contraseña (passwordHash nulo): el administrador no la conoce ni la define.
        // TODO(HU-SAT-03, 2026-11-08): al crear la cuenta, solicitar el envío del código de verificación al correo
        // para que el usuario defina su contraseña. Pendiente del servicio de restablecimiento (otro integrante);
        // debe ejecutarse de forma asíncrona y su falla no debe revertir el registro.
        docente.setActivo(true);
        docente.setFechaCreacion(LocalDateTime.now());

        Docente docenteGuardado = docenteRepository.save(docente);

        return convertirARespuesta(docenteGuardado);
    }

    /**
     * Convierte la entidad Docente en el DTO de salida.
     * Vive aquí porque solo este Service la usa y el mapeo es corto.
     */
    private DocenteResponse convertirARespuesta(Docente docente) {
        String nombreCompleto = String.join(" ",
                docente.getNombre(),
                docente.getApellidoPaterno(),
                docente.getApellidoMaterno() == null ? "" : docente.getApellidoMaterno()
        ).trim();

        return new DocenteResponse(
                docente.getId(),
                docente.getNombre(),
                docente.getApellidoPaterno(),
                docente.getApellidoMaterno(),
                nombreCompleto,
                docente.getDni(),
                docente.getUsername(),
                docente.getEmail(),
                docente.getTelefono(),
                docente.isActivo(),
                docente.getFechaCreacion()
        );
    }

    /** Valida la unicidad de DNI, correo y username exigida por HU-SAT-01. */
    private void validarUnicidad(String dni, String email) {
        if (usuarioRepository.existsByDniAndTipoUsuario(dni, Docente.class)) {
            throw new ConflictoException("Ya existe un docente registrado con el DNI indicado.");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("Ya existe un usuario registrado con el correo indicado.");
        }

        if (usuarioRepository.existsByUsername(extraerUsernameDeEmail(email))) {
            throw new ConflictoException("Ya existe un usuario registrado con ese identificador.");
        }
    }

    private String extraerUsernameDeEmail(String email) {
        return email.substring(0, email.indexOf('@'));
    }
}