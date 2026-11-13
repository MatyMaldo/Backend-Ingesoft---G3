package com.educoders.Gestion_Asistencia.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AsistenciaCierreJob {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaCierreJob.class);

    private final AsistenciaCierreService asistenciaCierreService;

    public AsistenciaCierreJob(AsistenciaCierreService asistenciaCierreService) {
        this.asistenciaCierreService = asistenciaCierreService;
    }

    @Scheduled(
            fixedDelayString = "${app.asistencia.cierre.intervalo-ms:60000}",
            initialDelayString = "${app.asistencia.cierre.initial-delay-ms:30000}")
    public void cerrarSesiones() {
        try {
            int procesadas = asistenciaCierreService.procesarSesiones();
            if (procesadas > 0) {
                log.info("Cierre de asistencia procesó {} sesiones.", procesadas);
            }
        } catch (Exception ex) {
            log.error("Falló la ejecución del cierre de asistencia", ex);
        }
    }
}
