package com.educoders.Infraestructura_Servicios.util;

/** Utilidades para convertir texto libre de búsqueda en patrones {@code LIKE} seguros. */
public final class PatronBusqueda {

    /** Carácter de escape que las consultas declaran con {@code ESCAPE '!'}. */
    public static final String TODO = "%";

    private PatronBusqueda() {
    }

    /**
     * Convierte el texto libre en un patrón LIKE en minúsculas que contiene el texto, escapando los
     * comodines ({@code %}, {@code _}) y el propio escape ({@code !}). Un texto vacío coincide con todo.
     */
    public static String contiene(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return TODO;
        }
        String escapado = busqueda.trim().toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escapado + "%";
    }
}
