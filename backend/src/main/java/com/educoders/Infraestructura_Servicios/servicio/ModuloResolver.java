package com.educoders.Infraestructura_Servicios.servicio;

import com.educoders.Infraestructura_Servicios.enums.Modulo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Traduce la ruta de un endpoint al módulo del sistema.
 * El mapeo es explícito y no depende de la versión de la API
 * (/api/v1, /api/v2, o sin versión).
 */
@Component
public class ModuloResolver {

    private record Regla(Predicate<String> condicion, Modulo modulo) {
    }

    private static final List<Regla> REGLAS = List.of(
            // Rutas específicas primero
            new Regla(p -> p.contains("/libretas") || p.contains("/revision-notas"), Modulo.NOTAS),
            new Regla(p -> p.startsWith("/evaluaciones"), Modulo.NOTAS),
            // Horarios
            new Regla(p -> p.startsWith("/aulas")
                    || p.startsWith("/bloques-horario")
                    || p.startsWith("/sesiones-clase")
                    || p.startsWith("/asignaciones-curso"), Modulo.HORARIOS),
            // Matrícula y pagos
            new Regla(p -> p.startsWith("/matriculas"), Modulo.MATRICULA),
            new Regla(p -> p.startsWith("/pagos") || p.startsWith("/compromisos-pago"), Modulo.PAGOS),
            // Asistencia
            new Regla(p -> p.startsWith("/asistencias"), Modulo.ASISTENCIA),
            // Autenticación
            new Regla(p -> p.startsWith("/auth"), Modulo.AUTENTICACION),
            // Usuarios
            new Regla(p -> p.startsWith("/usuarios")
                    || p.startsWith("/docentes")
                    || p.startsWith("/apoderados")
                    || p.startsWith("/estudiantes"), Modulo.USUARIOS),
            // Estructura académica
            new Regla(p -> p.startsWith("/grados")
                    || p.startsWith("/secciones")
                    || p.startsWith("/anios-academicos")
                    || p.startsWith("/cursos"), Modulo.ESTRUCTURA_ACADEMICA),
            // Administración / auditoría
            new Regla(p -> p.startsWith("/auditorias") || p.startsWith("/admin/logs-error"), Modulo.OTROS)
    );

    public Modulo resolver(String endpoint) {
        String ruta = normalizarRuta(endpoint);
        for (Regla regla : REGLAS) {
            if (regla.condicion().test(ruta)) {
                return regla.modulo();
            }
        }
        return Modulo.OTROS;
    }

    /** Quita /api y la versión (/v1, /v2, ...) para desacoplar las reglas de la versión. */
    private String normalizarRuta(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "/";
        }
        String ruta = endpoint.trim().toLowerCase();
        ruta = ruta.replaceFirst("^/api(/v\\d+)?", "");
        return ruta.isEmpty() ? "/" : ruta;
    }
}