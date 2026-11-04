package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.LogError;
import com.educoders.Seguridad_Auditoria.persistencia.LogErrorRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aísla la escritura del log de errores en su propia transacción,
 * de modo que un fallo al registrar el error nunca afecte la operación principal.
 */
@Component
public class LogErrorWriter {

    private final LogErrorRepository logErrorRepository;

    public LogErrorWriter(LogErrorRepository logErrorRepository) {
        this.logErrorRepository = logErrorRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardar(LogError logError) {
        logErrorRepository.save(logError);
    }
}