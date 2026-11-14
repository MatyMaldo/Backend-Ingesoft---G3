package com.educoders.Gestion_Matriz_Horarios.persistencia;

import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;

import java.time.LocalDate;

/**
 * Sesión ya registrada, reducida a lo que necesitan la vista previa y la generación para saber si una fecha de un
 * bloque ya existe y en qué estado: así no se cargan las sesiones completas ni sus relaciones.
 */
public record SesionExistente(Long bloqueHorarioId, LocalDate fecha, EstadoSesion estado) {
}
