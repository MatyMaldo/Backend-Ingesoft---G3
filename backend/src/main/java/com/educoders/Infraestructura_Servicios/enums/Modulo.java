package com.educoders.Infraestructura_Servicios.enums;

public enum Modulo {
    USUARIOS("Usuarios"),
    MATRICULA("Matrícula"),
    PAGOS("Pagos"),
    ASISTENCIA("Asistencia"),
    NOTAS("Notas"),
    HORARIOS("Horarios"),
    ESTRUCTURA_ACADEMICA("Configuración"),
    AUTENTICACION("Autenticación"),
    OTROS("Otros");

    private final String etiqueta;

    Modulo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * Resuelve de forma inteligente el módulo a partir del nombre de la clase
     * o la ruta de la API, evitando acoplamiento rígido de paquetes.
     */
    public static Modulo resolverModulo(String origen, String endpoint) {
        String textoAnalizable = ((origen != null ? origen : "") + " " + (endpoint != null ? endpoint : "")).toLowerCase();

        if (textoAnalizable.contains("pago") || textoAnalizable.contains("compromiso") || textoAnalizable.contains("pension")) {
            return PAGOS;
        }
        if (textoAnalizable.contains("matricula")) {
            return MATRICULA;
        }
        if (textoAnalizable.contains("auth") || textoAnalizable.contains("login") || textoAnalizable.contains("token")) {
            return AUTENTICACION;
        }
        if (textoAnalizable.contains("usuario") || textoAnalizable.contains("docente") || textoAnalizable.contains("apoderado") || textoAnalizable.contains("estudiante")) {
            return USUARIOS;
        }
        if (textoAnalizable.contains("asistencia")) {
            return ASISTENCIA;
        }
        if (textoAnalizable.contains("nota") || textoAnalizable.contains("evaluacion") || textoAnalizable.contains("libreta")) {
            return NOTAS;
        }
        if (textoAnalizable.contains("horario") || textoAnalizable.contains("bloque") || textoAnalizable.contains("aula")) {
            return HORARIOS;
        }
        if (textoAnalizable.contains("seccion") || textoAnalizable.contains("grado") || textoAnalizable.contains("anio")) {
            return ESTRUCTURA_ACADEMICA;
        }

        return OTROS;
    }
}