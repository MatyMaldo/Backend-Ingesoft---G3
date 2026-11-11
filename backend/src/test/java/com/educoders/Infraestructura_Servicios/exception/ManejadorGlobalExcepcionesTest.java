package com.educoders.Infraestructura_Servicios.exception;

import com.educoders.Gestion_Matriz_Horarios.exception.GuardadoBloquesConConflictosException;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GuardadoSeccionErroresResponse;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ManejadorGlobalExcepcionesTest {

    @Mock
    private LogErrorService logErrorService;

    @Test
    void guardadoConConflictosDevuelve409ConDetalle() {
        ManejadorGlobalExcepciones manejador =
                new ManejadorGlobalExcepciones(logErrorService);

        GuardadoSeccionErroresResponse detalle = new GuardadoSeccionErroresResponse(
                List.of(new GuardadoSeccionErroresResponse.ConflictoBloque(
                        1,
                        null,
                        0,
                        null,
                        10L,
                        4L,
                        DiaSemana.LUNES,
                        LocalTime.of(8, 0),
                        LocalTime.of(9, 30),
                        "CONFLICTO_AULA",
                        "El aula 101 está ocupada por 2do Grado B (Historia) el lunes de 08:00 a 09:30."
                )));

        ResponseEntity<com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse<
                GuardadoSeccionErroresResponse>> respuesta =
                manejador.manejarGuardadoConConflictos(
                        new GuardadoBloquesConConflictosException(detalle));

        assertEquals(409, respuesta.getStatusCode().value());
        assertNotNull(respuesta.getBody());
        assertEquals(detalle, respuesta.getBody().datos());
        assertEquals("No se guardó el horario: 1 conflicto(s).", respuesta.getBody().error());
    }
}
