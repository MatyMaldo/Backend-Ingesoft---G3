package com.educoders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Punto de entrada del prototipo de arquitectura de EduCoders.
 */
@SpringBootApplication
public class EduCodersApplication {

    public static void main(String[] args) {

        
        /* Prueba de encriptación BCrypt
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String plainPassword = "123456";
        String currentHash = "$2a$10$1J6URNqgqbtUQGpF8g5OWelFDcv5RM8rfPSwNYOksXGBgUqFBeVJu";
        
        System.out.println("==========================================");
        System.out.println("PRUEBA DE ENCRIPTACIÓN BCrypt");
        System.out.println("Contraseña en texto plano: " + plainPassword);
        System.out.println("Hash actual en BD: " + currentHash);
        System.out.println("¿El hash actual coincide con '123456'? " + encoder.matches(plainPassword, currentHash));
        
        // Generar nuevo hash
        String newHash = encoder.encode(plainPassword);
        System.out.println("Nuevo hash generado para '123456': " + newHash);
        System.out.println("¿El nuevo hash funciona? " + encoder.matches(plainPassword, newHash));
        System.out.println("==========================================");*/

        SpringApplication.run(EduCodersApplication.class, args);
    }
}
