package com.educoders.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj de la aplicación. Permite usar un reloj fijo en pruebas sin cambiar
 * la fecha del sistema.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
