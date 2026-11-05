package com.educoders.Seguridad_Auditoria.servicio;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtExpiracionTest {

    @Test
    void rechazarJwtVencido() {
        String secreto = "clave-de-prueba-educoders-jwt-segura-123456789";
        SecretKey clave = Keys.hmacShaKeyFor(
                secreto.getBytes(StandardCharsets.UTF_8)
        );

        long ahora = System.currentTimeMillis();

        String token = Jwts.builder()
                .subject("admin@colegio.com")
                .issuedAt(new Date(ahora - 120000))
                .expiration(new Date(ahora - 60000))
                .signWith(clave)
                .compact();

        JwtService jwtService = new JwtService(secreto, 3600000);

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.validarToken(token)
        );
    }
}