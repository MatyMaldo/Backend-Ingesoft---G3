package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ActualizarUsuarioRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.UsuarioResponse;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de HU-SAT-02.
 * Responsabilidad única: consultar, editar, desactivar y reactivar usuarios.
 * Nunca elimina usuarios (RT010).
 */
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(String tipoUsuario, Boolean activo, Pageable pageable) {
        if (tipoUsuario != null && !tipoUsuario.isBlank()) {
            return usuarioRepository.findByTipoUsuario(tipoUsuario.toUpperCase(), pageable)
                    .map(this::convertirARespuesta);
        }
        if (activo != null) {
            return usuarioRepository.findByActivo(activo, pageable)
                    .map(this::convertirARespuesta);
        }
        return usuarioRepository.findAll(pageable)
                .map(this::convertirARespuesta);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long idUsuario) {
        return convertirARespuesta(obtenerEntidad(idUsuario));
    }

    /**
     * HU-SAT-02 — Regla de negocio: edición PARCIAL. Solo se actualizan los
     * campos enviados (no nulos). Los campos no enviados quedan intactos.
     * Si se envía un DNI o correo, se valida su unicidad excluyendo al propio
     * usuario. El rol y la contraseña no se modifican por esta vía.
     */
    @Override
    @Transactional
    public UsuarioResponse actualizar(Long idUsuario, ActualizarUsuarioRequest solicitud) {
        Usuario usuario = obtenerEntidad(idUsuario);

        if (tieneTexto(solicitud.nombre())) {
            usuario.setNombre(solicitud.nombre());
        }
        if (tieneTexto(solicitud.apellidoPaterno())) {
            usuario.setApellidoPaterno(solicitud.apellidoPaterno());
        }
        if (solicitud.apellidoMaterno() != null) {
            // El apellido materno puede quedar vacío o nulo legítimamente.
            usuario.setApellidoMaterno(solicitud.apellidoMaterno());
        }

        if (tieneTexto(solicitud.dni()) && !solicitud.dni().equals(usuario.getDni())) {
            if (usuarioRepository.existsByDniAndIdNot(solicitud.dni(), idUsuario)) {
                throw new ConflictoException("Ya existe otro usuario con el DNI indicado.");
            }
            usuario.setDni(solicitud.dni());
            usuario.setUsername(solicitud.dni());   // el username sigue al DNI
        }

        if (tieneTexto(solicitud.email()) && !solicitud.email().equals(usuario.getEmail())) {
            if (usuarioRepository.existsByEmailAndIdNot(solicitud.email(), idUsuario)) {
                throw new ConflictoException("Ya existe otro usuario con el correo indicado.");
            }
            usuario.setEmail(solicitud.email());
        }

        if (solicitud.telefono() != null) {
            usuario.setTelefono(solicitud.telefono());
        }

        return convertirARespuesta(usuarioRepository.save(usuario));
    }

    /**
     * Desactivar no elimina. El usuario
     * conserva sus registros históricos y queda bloqueado para iniciar
     * sesión.
     */
    @Override
    @Transactional
    public UsuarioResponse desactivar(Long idUsuario) {
        Usuario usuario = obtenerEntidad(idUsuario);

        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("El usuario ya se encuentra inactivo.");
        }

        usuario.setActivo(false);
        return convertirARespuesta(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse reactivar(Long idUsuario) {
        Usuario usuario = obtenerEntidad(idUsuario);

        if (usuario.isActivo()) {
            throw new ReglaNegocioException("El usuario ya se encuentra activo.");
        }

        usuario.setActivo(true);
        return convertirARespuesta(usuarioRepository.save(usuario));
    }

    private Usuario obtenerEntidad(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> RecursoNoEncontradoException.de("usuario", idUsuario));
    }

    /** @return true si el texto no es nulo ni está en blanco. */
    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    /**
     * Convierte la entidad Usuario en su DTO de salida.
     * Vive aquí porque solo este Service la usa y el mapeo es corto.
     */
    private UsuarioResponse convertirARespuesta(Usuario usuario) {
        String nombreCompleto = String.join(" ",
                usuario.getNombre(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno() == null ? "" : usuario.getApellidoMaterno()
        ).trim();

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno(),
                nombreCompleto,
                usuario.getDni(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getClass().getSimpleName().toUpperCase(),
                usuario.isActivo(),
                usuario.getFechaCreacion()
        );
    }
}
