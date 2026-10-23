package com.educoders.Seguridad_Auditoria.servicio;


import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.expiration-ms:3600000}") long expiracionMs
    ) {
        if (secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "La clave JWT debe tener al menos 32 bytes"
            );
        }

        this.clave = Keys.hmacShaKeyFor(
                secreto.getBytes(StandardCharsets.UTF_8)
        );
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(Usuario usuario, String rol) {
        Date ahora = new Date();
        Date vencimiento = new Date(
                ahora.getTime() + expiracionMs
        );

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(vencimiento)
                .signWith(clave)
                .compact();
    }

    public Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}