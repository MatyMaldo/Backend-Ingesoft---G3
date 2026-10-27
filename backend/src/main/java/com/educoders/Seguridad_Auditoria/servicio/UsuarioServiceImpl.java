package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ActualizarUsuarioRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.UsuarioResponse;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.BimestreRepository;
import com.educoders.Infraestructura_Servicios.enums.NombreBimestre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reglas de negocio de HU-SAT-02.
 * Responsabilidad única: consultar, editar, desactivar y reactivar usuarios.
 * Nunca elimina usuarios (RT010).
 */
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SeccionRepository seccionRepository;
    private final BimestreRepository bimestreRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              SeccionRepository seccionRepository,
                              BimestreRepository bimestreRepository) {
        this.usuarioRepository = usuarioRepository;
        this.seccionRepository = seccionRepository;
        this.bimestreRepository = bimestreRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(String tipoUsuario, Boolean activo, Pageable pageable) {
        if (tipoUsuario != null && !tipoUsuario.isBlank()) {
            Class<?> clase = obtenerClasePorTipo(tipoUsuario.toUpperCase());
            if (activo != null) {
                return usuarioRepository.findByTipoUsuarioAndActivo(clase, activo, pageable)
                        .map(this::convertirARespuesta);
            }
            return usuarioRepository.findByTipoUsuario(clase, pageable)
                    .map(this::convertirARespuesta);
        }
        if (activo != null) {
            return usuarioRepository.findByActivo(activo, pageable)
                    .map(this::convertirARespuesta);
        }
        return usuarioRepository.findAll(pageable)
                .map(this::convertirARespuesta);
    }

    private Class<?> obtenerClasePorTipo(String tipo) {
        return switch (tipo) {
            case "DOCENTE" -> com.educoders.Seguridad_Auditoria.dominio.Docente.class;
            case "APODERADO" -> com.educoders.Seguridad_Auditoria.dominio.Apoderado.class;
            case "ESTUDIANTE" -> com.educoders.Seguridad_Auditoria.dominio.Estudiante.class;
            case "ADMINISTRADOR" -> com.educoders.Seguridad_Auditoria.dominio.Administrador.class;
            default -> throw new ReglaNegocioException("Tipo de usuario no válido: " + tipo);
        };
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

        if (usuario instanceof Administrador) {
            throw new ReglaNegocioException("No se pueden modificar usuarios administrador.");
        }

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
            if (usuarioRepository.existsByDniAndTipoUsuarioAndIdNot(
                    solicitud.dni(), usuario.getClass(), idUsuario)) {
                throw new ConflictoException("Ya existe otro usuario con el DNI indicado para este tipo de usuario.");
            }
            usuario.setDni(solicitud.dni());
        }

        if (tieneTexto(solicitud.email()) && !solicitud.email().equals(usuario.getEmail())) {
            if (usuarioRepository.existsByEmailAndIdNot(solicitud.email(), idUsuario)) {
                throw new ConflictoException("Ya existe otro usuario con el correo indicado.");
            }
            usuario.setEmail(solicitud.email());
            // Actualizar username basado en el nuevo email
            usuario.setUsername(extraerUsernameDeEmail(solicitud.email()));
        }

        if (solicitud.telefono() != null) {
            usuario.setTelefono(solicitud.telefono());
        }

        return convertirARespuesta(usuarioRepository.save(usuario));
    }

    /**
     * Desactivar no elimina. El usuario
     * conserva sus registros históricos y queda bloqueado para iniciar
     * sesión. Si es un docente, se verifica si es tutor de alguna sección
     * y se informa al administrador.
     */
    @Override
    @Transactional
    public UsuarioResponse desactivar(Long idUsuario) {
        Usuario usuario = obtenerEntidad(idUsuario);

        if (usuario instanceof Administrador) {
            throw new ReglaNegocioException("No se pueden desactivar usuarios administrador.");
        }

        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("El usuario ya se encuentra inactivo.");
        }

        // Si es docente, verificar secciones donde es tutor
        if (usuario instanceof Docente docente) {
            List<Seccion> seccionesComoTutor = seccionRepository.findByTutorId(idUsuario);

            if (!seccionesComoTutor.isEmpty()) {
                // Desactivar usuario
                usuario.setActivo(false);
                usuarioRepository.save(usuario);

                // Solo remover tutor de secciones cuyo año académico esté abierto
                List<UsuarioResponse.SeccionAfectadaInfo> seccionesInfo = new java.util.ArrayList<>();
                LocalDate hoy = LocalDate.now();

                for (Seccion seccion : seccionesComoTutor) {
                    // Verificar si el año académico de esta sección está abierto
                    var primerBimestre = bimestreRepository
                            .findFirstByNombreAndAnioAcademicoIdOrderByAnioAcademicoAnioDesc(
                                    NombreBimestre.BIM_1,
                                    seccion.getAnioAcademico().getId());

                    boolean anioAbierto = primerBimestre.isPresent()
                            && hoy.isBefore(primerBimestre.get().getFechaInicio());

                    if (anioAbierto) {
                        // Año académico abierto: remover tutor
                        seccion.setTutor(null);
                        seccionRepository.save(seccion);

                        seccionesInfo.add(new UsuarioResponse.SeccionAfectadaInfo(
                            seccion.getId(),
                            seccion.getNombre(),
                            seccion.getGrado().getNombre(),
                            seccion.getAnioAcademico().getAnio()
                        ));
                    }
                    // Si el año está cerrado, NO se remueve el tutor (se mantiene histórico)
                }

                return new UsuarioResponse(
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getApellidoPaterno(),
                    usuario.getApellidoMaterno(),
                    String.join(" ",
                        usuario.getNombre(),
                        usuario.getApellidoPaterno(),
                        usuario.getApellidoMaterno() == null ? "" : usuario.getApellidoMaterno()
                    ).trim(),
                    usuario.getDni(),
                    usuario.getUsername(),
                    usuario.getEmail(),
                    usuario.getTelefono(),
                    usuario.getClass().getSimpleName().toUpperCase(),
                    false,
                    usuario.getFechaCreacion(),
                    seccionesInfo.isEmpty() ? null : seccionesInfo
                );
            }
        }

        usuario.setActivo(false);
        return convertirARespuesta(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse reactivar(Long idUsuario) {
        Usuario usuario = obtenerEntidad(idUsuario);

        if (usuario instanceof Administrador) {
            throw new ReglaNegocioException("No se pueden reactivar usuarios administrador.");
        }

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

    /** Extrae la parte local del email (antes del @) para usar como username. */
    private String extraerUsernameDeEmail(String email) {
        return email.substring(0, email.indexOf('@'));
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
