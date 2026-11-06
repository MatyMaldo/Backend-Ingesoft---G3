package com.educoders.Seguridad_Auditoria.servicio;


import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
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
                .claim("credencial", huellaCredencial(usuario))
                .issuedAt(ahora)
                .expiration(vencimiento)
                .signWith(clave)
                .compact();
    }

    public String huellaCredencial(Usuario usuario) {
        String valor = "educoders:credencial:" + usuario.getId() + ":" + usuario.getPasswordHash();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave.getEncoded(), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular la huella de la credencial", ex);
        }
    }

    public Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}