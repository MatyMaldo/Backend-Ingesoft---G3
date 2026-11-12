package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.util.PatronBusqueda;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Auditoria;
import com.educoders.Seguridad_Auditoria.dominio.DetalleAuditoria;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.AuditoriaRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.AuditoriaResponseDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleAuditoriaDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.UsuarioAuditoriaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaServiceImpl.class);
    static final String AUTOR_NO_DETERMINADO = "No determinado";
    static final String ROL_NO_DETERMINADO = "NO_DETERMINADO";
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaServiceImpl(AuditoriaRepository auditoriaRepository,
                                UsuarioRepository usuarioRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Regla HU-SAT-04 (Esc. 2, atomicidad): el registro de auditoría se confirma junto con la operación que lo
     * origina o ninguno de los dos se aplica. Por eso se une a la transacción de quien llama ({@code MANDATORY}:
     * sin transacción abierta falla, en lugar de guardar la auditoría por separado) y NO captura errores: si no
     * puede guardarse, la excepción cancela la operación.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarAuditoria(RegistrarAuditoriaRequestDTO dto) {
        Long usuarioIdFinal = dto.isAutorFijo()
                ? dto.getUsuarioId()
                : obtenerUsuarioIdAutenticado(dto.getUsuarioId());

        Auditoria auditoria = new Auditoria();
        auditoria.setFechaHora(LocalDateTime.now());
        auditoria.setUsuarioId(usuarioIdFinal);
        auditoria.setCategoria(dto.getCategoria() != null ? dto.getCategoria() : CategoriaAuditoria.SISTEMA);
        auditoria.setAccion(dto.getAccion());
        auditoria.setEntidad(dto.getEntidad());
        auditoria.setEntidadId(dto.getEntidadId());
        auditoria.setDescripcion(dto.getDescripcion());

        if (dto.getDetalles() != null) {
            for (DetalleAuditoriaDTO detalleDto : dto.getDetalles()) {
                DetalleAuditoria detalle = new DetalleAuditoria(
                        detalleDto.getCampo(),
                        detalleDto.getValorAnterior(),
                        detalleDto.getValorNuevo()
                );
                auditoria.agregarDetalle(detalle);
            }
        }

        auditoriaRepository.saveAndFlush(auditoria);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> listarAuditorias(CategoriaAuditoria categoria,
                                                       TipoAccionAuditoria accion,
                                                       LocalDateTime fechaInicio, LocalDateTime fechaFin,
                                                       String busqueda, Pageable pageable) {
        Page<Auditoria> auditorias = auditoriaRepository.buscarConFiltros(
                categoria, accion, fechaInicio, fechaFin, PatronBusqueda.contiene(busqueda), pageable);
        Map<Long, Usuario> usuarios = cargarUsuarios(auditorias.getContent());
        return auditorias.map(auditoria -> mapearAResponse(auditoria, usuarios));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> obtenerHistorialPorEntidad(String entidad, Long entidadId, Pageable pageable) {
        Page<Auditoria> historial = auditoriaRepository
                .findByEntidadAndEntidadIdOrderByFechaHoraAsc(entidad, entidadId, pageable);
        Map<Long, Usuario> usuarios = cargarUsuarios(historial.getContent());
        return historial.map(auditoria -> mapearAResponse(auditoria, usuarios));
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriaResponseDTO obtenerPorId(Long id) {
        Auditoria auditoria = auditoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Auditoría no encontrada"));

        Long usuarioId = auditoria.getUsuarioId();
        Usuario usuario = usuarioId != null
                ? usuarioRepository.findById(usuarioId).orElse(null)
                : null;

        return mapearAResponse(auditoria, construirAutor(auditoria, usuario));
    }

    private Long obtenerUsuarioIdAutenticado(Long idProporcionado) {
        // 1) Prioridad al usuario autenticado en el token (no confiable en el payload).
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                Object principalObj = auth.getPrincipal();

                // El filtro JWT actual expone la entidad Usuario como principal.
                if (principalObj instanceof Usuario usuario) {
                    return usuario.getId();
                }

                // Compatibilidad con implementaciones que exponen el id como texto.
                String principal = principalObj.toString();
                if (!principal.isBlank() && !"anonymousUser".equalsIgnoreCase(principal)) {
                    try {
                        return Long.valueOf(principal);
                    } catch (NumberFormatException ignored) {
                        // principal no numérico; se ignora
                    }
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo extraer el usuario del SecurityContext: {}", e.getMessage());
        }
        // 2) Fallback: id proporcionado (procesos internos / sistema).
        return idProporcionado;
    }

    private Map<Long, Usuario> cargarUsuarios(List<Auditoria> auditorias) {
        Set<Long> ids = auditorias.stream()
                .map(Auditoria::getUsuarioId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        return usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, usuario -> usuario));
    }

    private AuditoriaResponseDTO mapearAResponse(Auditoria auditoria,
                                                 Map<Long, Usuario> usuarios) {
        Long usuarioId = auditoria.getUsuarioId();
        Usuario usuario = usuarioId != null ? usuarios.get(usuarioId) : null;
        return mapearAResponse(auditoria, construirAutor(auditoria, usuario));
    }

    /**
     * Autor mostrado de un registro. Sin usuario, una acción de sesión ({@code LOGIN}) solo puede ser un intento
     * fallido con un correo que no pertenece a ninguna cuenta: el autor es "No determinado" (HU-SAT-04). Cualquier
     * otro registro sin usuario lo ejecutó el sistema.
     */
    private UsuarioAuditoriaDTO construirAutor(Auditoria auditoria, Usuario usuario) {
        if (auditoria.getUsuarioId() == null && auditoria.getAccion() == TipoAccionAuditoria.LOGIN) {
            return new UsuarioAuditoriaDTO(null, AUTOR_NO_DETERMINADO, ROL_NO_DETERMINADO);
        }
        return construirUsuario(auditoria.getUsuarioId(), usuario);
    }

    private AuditoriaResponseDTO mapearAResponse(Auditoria auditoria,
                                                 UsuarioAuditoriaDTO usuario) {
        return new AuditoriaResponseDTO(
                auditoria.getId(),
                auditoria.getFechaHora(),
                auditoria.getUsuarioId(),
                usuario,
                auditoria.getCategoria(),
                auditoria.getAccion().name(),
                auditoria.getEntidad(),
                auditoria.getEntidadId(),
                auditoria.getDescripcion(),
                auditoria.getDetalles().stream()
                        .map(d -> new DetalleAuditoriaDTO(d.getCampo(), d.getValorAnterior(), d.getValorNuevo()))
                        .collect(Collectors.toList())
        );
    }

    private UsuarioAuditoriaDTO construirUsuario(Long usuarioId, Usuario usuario) {
        if (usuarioId == null) {
            return new UsuarioAuditoriaDTO(null, "Sistema", "SISTEMA");
        }
        if (usuario == null) {
            return new UsuarioAuditoriaDTO(usuarioId, "Usuario #" + usuarioId, null);
        }

        String nombre = usuario.getNombre() + " " + usuario.getApellidoPaterno();
        if (usuario.getApellidoMaterno() != null && !usuario.getApellidoMaterno().isBlank()) {
            nombre += " " + usuario.getApellidoMaterno();
        }

        return new UsuarioAuditoriaDTO(usuarioId, nombre, obtenerRol(usuario));
    }

    private String obtenerRol(Usuario usuario) {
        if (usuario instanceof Administrador) return "ADMINISTRADOR";
        if (usuario instanceof Docente) return "DOCENTE";
        if (usuario instanceof Apoderado) return "APODERADO";
        if (usuario instanceof Estudiante) return "ESTUDIANTE";
        return null;
    }
}