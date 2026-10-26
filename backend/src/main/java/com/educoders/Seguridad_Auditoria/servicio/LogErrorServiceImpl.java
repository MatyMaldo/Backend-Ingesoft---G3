package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.LogError;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.LogErrorRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.LogErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LogErrorServiceImpl implements LogErrorService {

    private static final Logger REGISTRO = LoggerFactory.getLogger(LogErrorServiceImpl.class);

    private final LogErrorRepository logErrorRepository;
    private final UsuarioRepository usuarioRepository;

    public LogErrorServiceImpl(LogErrorRepository logErrorRepository, UsuarioRepository usuarioRepository) {
        this.logErrorRepository = logErrorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarError(String origen, String tipoError, String mensaje, String detalle,
                               String endpoint, String metodoHttp, Short codigoEstado, Long usuarioId) {
        try {
            Usuario usuario = null;
            if (usuarioId != null) {
                usuario = usuarioRepository.findById(usuarioId).orElse(null);
            }

            String mensajeSeguro = (mensaje != null && mensaje.length() > 2000)
                    ? mensaje.substring(0, 1997) + "..."
                    : mensaje;

            LogError logError = new LogError(
                    LocalDateTime.now(),
                    usuario,
                    tipoError,
                    mensajeSeguro,
                    detalle,
                    endpoint,
                    metodoHttp,
                    codigoEstado
            );

            logErrorRepository.save(logError);
        } catch (Exception ex) {
            REGISTRO.error("No se pudo guardar el registro de error en la base de datos: ", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LogErrorResponse> listarErrores(LocalDateTime fechaInicio, LocalDateTime fechaFin, Pageable pageable) {
        return logErrorRepository.buscarConFiltros(fechaInicio, fechaFin, pageable)
                .map(LogErrorResponse::desdeEntidad);
    }
}