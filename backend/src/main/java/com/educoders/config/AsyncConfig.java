package com.educoders.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor acotado para las operaciones masivas asíncronas.
 * <p>
 * Un pool acotado evita que muchas operaciones simultáneas consuman toda la
 * memoria o los hilos del servidor. Cuando la cola está llena, el executor
 * rechaza la tarea y la API responde 503 en lugar de dejar la petición colgada.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "operacionesMasivasExecutor")
    public ThreadPoolTaskExecutor operacionesMasivasExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("operacion-masiva-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Executor para enviar correos sin hacer esperar la petición HTTP: el SMTP (conexión, STARTTLS y
     * autenticación) tarda segundos, y emitir un compromiso o validar un pago no debe esperarlo.
     */
    @Bean(name = "correosExecutor")
    public ThreadPoolTaskExecutor correosExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("correo-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
