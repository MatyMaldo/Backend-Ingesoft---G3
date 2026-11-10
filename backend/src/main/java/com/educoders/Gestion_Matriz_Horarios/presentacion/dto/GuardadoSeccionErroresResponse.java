package com.educoders.Gestion_Matriz_Horarios.presentacion.dto;

import com.educoders.Infraestructura_Servicios.enums.DiaSemana;

import java.time.LocalTime;
import java.util.List;

/**
 * Detalle de los problemas que impidieron guardar el horario completo de una
 * sección. El guardado sigue siendo todo o nada: si hay errores, no se persiste
 * ningún cambio.
 */
public record GuardadoSeccionErroresResponse(
        List<ConflictoBloque> errores
) {
    public GuardadoSeccionErroresResponse {
        errores = List.copyOf(errores);
    }

    public record ConflictoBloque(
            Integer indice,
            Long bloqueId,
            Integer indiceConflicto,
            Long bloqueConflictoId,
            Long asignacionCursoId,
            Long aulaId,
            DiaSemana diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin,
            String tipo,
            String mensaje
    ) {
    }
}
