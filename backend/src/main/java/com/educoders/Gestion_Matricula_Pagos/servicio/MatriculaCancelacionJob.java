package com.educoders.Gestion_Matricula_Pagos.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cancela diariamente las matrículas pendientes cuyo periodo de matrícula
 * ya terminó y que no tienen un compromiso de matrícula pagado.
 */
@Component
public class MatriculaCancelacionJob {

    private static final Logger LOG = LoggerFactory.getLogger(MatriculaCancelacionJob.class);

    private final MatriculaService matriculaService;

    public MatriculaCancelacionJob(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void ejecutar() {
        int canceladas = matriculaService.cancelarPendientesPorPeriodoVencido();
        if (canceladas > 0) {
            LOG.info("Cancelación automática de matrículas: {} matrícula(s) cancelada(s).", canceladas);
        }
    }
}
