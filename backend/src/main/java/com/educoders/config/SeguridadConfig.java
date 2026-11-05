
package com.educoders.config;

import com.educoders.Seguridad_Auditoria.seguridad.JwtAuthenticationFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter jwtFilter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtFilter);

        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                        {


                            response.sendError(
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "No autenticado"


                            );
                        })
                                /*
                                response.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "No autenticado"
                                )
                        )*/
                       .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "No tiene permisos para acceder a este recurso"
                                )
                        )

                )

                .authorizeHttpRequests(auth -> auth
                        // Permitir los despachos internos de errores
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // Login público
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/login"
                        ).permitAll()

                        // --- HU-EVAL-01 ---
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/evaluaciones"
                        ).hasAnyRole("ADMINISTRADOR", "DOCENTE")

                        // --- HU-EVAL-02 ---
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/evaluaciones/*/notas"
                        ).hasAnyRole("ADMINISTRADOR", "DOCENTE")

                        // --- HU-EVAL-03 ---
                        // Permite que estudiantes y apoderados consulten las notas
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/estudiantes/*/revision-notas/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "ESTUDIANTE",
                                "APODERADO"
                        )// HU-SAT-07: Consulta de usuarios solo para administradores
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/usuarios",
                                "/api/v1/usuarios/*"
                        ).hasRole("ADMINISTRADOR")
                        // Gestión administrativa de usuarios
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/usuarios/**"
                        ).hasRole("ADMINISTRADOR")

                        // Auditorías
                        .requestMatchers(
                                "/api/v1/auditorias/**"
                        ).hasRole("ADMINISTRADOR")

                        // Logs de errores
                        .requestMatchers(
                                "/api/v1/admin/logs-error/**",
                                "/api/admin/logs-error/**"
                        ).hasRole("ADMINISTRADOR")
                        // Gestión de grados y secciones
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/grados",
                                "/api/v1/secciones"
                        ).hasRole("ADMINISTRADOR")

                        // HU-EVAL-04 y HU-EVAL-05:
                        // Consulta y descarga de libretas de notas.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/estudiantes/*/libretas",
                                "/api/v1/estudiantes/*/libretas/**"
                        ).hasAnyRole(
                                "ESTUDIANTE",
                                "APODERADO"
                        )
                                // HU-GEMIH-03 y HU-GEMIH-04:
// Consultas administrativas de horarios
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/sesiones-clase",
                                        "/api/v1/sesiones-clase/docente/*",
                                        "/api/v1/sesiones-clase/*"
                                ).hasRole("ADMINISTRADOR")

// HU-GEMIH-05, HU-GEMIH-06 y HU-GEMIH-07:
// Consultas de horarios propios
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/sesiones-clase/mi-horario/docente"
                                ).hasRole("DOCENTE")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/sesiones-clase/mi-horario/estudiante"
                                ).hasRole("ESTUDIANTE")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/sesiones-clase/apoderado/*"
                                ).hasRole("APODERADO")


                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/visualizadores-horario/secciones/*",
                                        "/api/v1/visualizadores-horario/docentes/*"
                                ).hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/visualizadores-horario/mi-horario/docente"
                                ).hasRole("DOCENTE")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/visualizadores-horario/mi-horario/estudiante"
                                ).hasRole("ESTUDIANTE")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/visualizadores-horario/apoderado/*"
                                ).hasRole("APODERADO")


                                .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
