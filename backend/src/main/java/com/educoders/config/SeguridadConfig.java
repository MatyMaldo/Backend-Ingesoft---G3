
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            Object codigo = request.getAttribute(JwtAuthenticationFilter.CODIGO_RECHAZO);
                            if (codigo == null) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autenticado");
                                return;
                            }
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"codigo\":\"" + codigo + "\"}");
                        })
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

                        // Restablecimiento de contraseña público
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/password/**"
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


                        // --- HU-EVAL-07 ---
                        // Registrar calificaciones finales por competencia.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/notas-competencias"
                        ).hasRole("DOCENTE")

                        // --- HU-EVAL-03 ---
                        // Permite que estudiantes y apoderados consulten las notas
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/estudiantes/*/revision-notas/**"
                        ).hasAnyRole(
                                "ADMINISTRADOR",
                                "ESTUDIANTE",
                                "APODERADO"
                        )

                        // HU-GEMIH-07: estudiantes a cargo del apoderado autenticado
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/usuarios/apoderados/me/estudiantes"
                        ).hasRole("APODERADO")

                        // HU-SAT-07: Consulta de usuarios solo para administradores
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

                        // Carga masiva de usuarios
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/usuarios/carga-masiva/**"
                        ).hasRole("ADMINISTRADOR")

                        // Auditorías
                        .requestMatchers(
                                "/api/v1/auditorias/**"
                        ).hasRole("ADMINISTRADOR")

                        // Operaciones masivas asíncronas
                        .requestMatchers(
                                "/api/v1/operaciones-masivas/**"
                        ).hasRole("ADMINISTRADOR")

                        // Asistencia
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/asistencias/sesiones/*",
                                "/api/v1/asistencias/docente/mi-asistencia"
                        ).hasRole("DOCENTE")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/asistencias/sesiones/*/estudiantes",
                                "/api/v1/asistencias/sesiones/*/docente"
                        ).hasRole("DOCENTE")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/asistencias/estudiante/mi-asistencia"
                        ).hasRole("ESTUDIANTE")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/asistencias/apoderado/*"
                        ).hasRole("APODERADO")

                        .requestMatchers(
                                "/api/v1/asistencias/**"
                        ).denyAll()

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


                                // Listado administrativo y procesamiento de matrículas
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/matriculas"
                                ).hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/matriculas/cancelar-pendientes-por-periodo",
                                        "/api/v1/compromisos-pago/matricula/lote",
                                        "/api/v1/compromisos-pago/matricula/lote/async",
                                        "/api/v1/compromisos-pago/pension/lote",
                                        "/api/v1/compromisos-pago/pension/lote/async"
                                ).hasRole("ADMINISTRADOR")

                                // HU-GEMIH-02: solo el administrador gestiona la
                                // configuración de horarios (asignaciones, bloques y aulas).
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/asignaciones-curso",
                                        "/api/v1/asignaciones-curso/**",
                                        "/api/v1/bloques-horario",
                                        "/api/v1/bloques-horario/**",
                                        "/api/v1/aulas",
                                        "/api/v1/aulas/**",
                                        "/api/v1/sesiones-clase/preview",
                                        "/api/v1/sesiones-clase/generar",
                                        "/api/v1/sesiones-clase/generar/async"
                                ).hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/sesiones-clase/*/omision"
                                ).hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/asignaciones-curso/**",
                                        "/api/v1/bloques-horario/**",
                                        "/api/v1/aulas/**"
                                ).hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/asignaciones-curso/**",
                                        "/api/v1/bloques-horario/**",
                                        "/api/v1/aulas/**"
                                ).hasRole("ADMINISTRADOR")


                                .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "https://proyecto-ingesoft-g3-olive.vercel.app",
                "http://localhost:5173"
        ));

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        );

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
