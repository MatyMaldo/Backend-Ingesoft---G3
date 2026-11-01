
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
                                response.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "No autenticado"
                                )
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/login"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/evaluaciones"
                        ).hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/evaluaciones/*/notas"
                        ).hasAnyRole("ADMIN", "DOCENTE")
                        .anyRequest().hasRole("ADMIN")
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
