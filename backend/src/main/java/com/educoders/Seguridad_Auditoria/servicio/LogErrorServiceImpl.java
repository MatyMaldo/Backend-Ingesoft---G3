package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.Modulo;
import com.educoders.Infraestructura_Servicios.enums.TipoError;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.servicio.ModuloResolver;
import com.educoders.Infraestructura_Servicios.util.PatronBusqueda;
import com.educoders.Seguridad_Auditoria.dominio.LogError;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.LogErrorRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LogErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;

@Service
public class LogErrorServiceImpl implements LogErrorService {

    private static final Logger REGISTRO = LoggerFactory.getLogger(LogErrorServiceImpl.class);

    private final LogErrorRepository logErrorRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogErrorWriter logErrorWriter;
    private final ModuloResolver moduloResolver;

    public LogErrorServiceImpl(LogErrorRepository logErrorRepository,
                               UsuarioRepository usuarioRepository,
                               LogErrorWriter logErrorWriter,
                               ModuloResolver moduloResolver) {
        this.logErrorRepository = logErrorRepository;
        this.usuarioRepository = usuarioRepository;
        this.logErrorWriter = logErrorWriter;
        this.moduloResolver = moduloResolver;
    }

    @Override
    public void registrarError(String origen, TipoError tipoError, String mensaje, String detalle,
                               String endpoint, String metodoHttp, Short codigoEstado, Long usuarioId) {
        try {
            Long idUsuarioFinal = resolverUsuarioId(usuarioId);

            Usuario usuario = null;
            if (idUsuarioFinal != null) {
                usuario = usuarioRepository.findById(idUsuarioFinal).orElse(null);
            }

            String mensajeSeguro = (mensaje != null && mensaje.length() > 2000)
                    ? mensaje.substring(0, 1997) + "..."
                    : mensaje;

            Modulo modulo = moduloResolver.resolver(endpoint);

            LogError logError = new LogError(
                    LocalDateTime.now(),
                    usuario,
                    modulo,
                    tipoError,
                    mensajeSeguro,
                    detalle,
                    endpoint,
                    metodoHttp,
                    codigoEstado
            );

            logErrorWriter.guardar(logError);
        } catch (Exception ex) {
            // La captura del error nunca debe interrumpir la operación que lo originó.
            REGISTRO.error("No se pudo guardar el registro de error en la base de datos: ", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LogErrorResponse> listarErrores(LocalDateTime fechaInicio, LocalDateTime fechaFin,
                                                String modulo, String busqueda, Pageable pageable) {
        Modulo moduloEnum = parsearModulo(modulo);
        if (modulo != null && !modulo.isBlank() && moduloEnum == null) {
            throw new ReglaNegocioException("Módulo no válido: " + modulo);
        }
        return logErrorRepository.buscarConFiltros(fechaInicio, fechaFin, moduloEnum,
                        PatronBusqueda.contiene(busqueda), pageable)
                .map(LogErrorResponse::desdeEntidad);
    }

    /** Si no llega un id explícito, toma el usuario autenticado del SecurityContext. */
    private Long resolverUsuarioId(Long usuarioId) {
        if (usuarioId != null) {
            return usuarioId;
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof Usuario usuario) {
                return usuario.getId();
            }
        } catch (Exception ignored) {
            // Sin contexto de seguridad (procesos batch/sistema); se registra sin usuario.
        }
        return null;
    }

    /** Acepta el nombre del enum (NOTAS) o la etiqueta (Notas / Configuración). */
    private Modulo parsearModulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String buscado = normalizar(valor);
        for (Modulo modulo : Modulo.values()) {
            if (normalizar(modulo.name()).equals(buscado)
                    || normalizar(modulo.getEtiqueta()).equals(buscado)) {
                return modulo;
            }
        }
        return null;
    }

    private String normalizar(String valor) {
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();
    }
}