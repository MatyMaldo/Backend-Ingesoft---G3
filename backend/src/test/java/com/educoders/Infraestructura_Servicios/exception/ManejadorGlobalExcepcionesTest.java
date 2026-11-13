package com.educoders.Infraestructura_Servicios.exception;

import com.educoders.Gestion_Matriz_Horarios.exception.GuardadoBloquesConConflictosException;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GuardadoSeccionErroresResponse;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalTime;
import java.util.List;

import com.educoders.Infraestructura_Servicios.enums.TipoError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ManejadorGlobalExcepcionesTest {

    @Mock
    private LogErrorService logErrorService;

    @Test
    void rutaInexistenteResponde404SinRegistrarseEnElLogDeErrores() {
        ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones(logErrorService);

        ResponseEntity<com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse<Void>> respuesta =
                manejador.manejarRutaNoEncontrada(
                        new NoResourceFoundException(HttpMethod.GET, "/api/v1/no-existe"));

        assertEquals(404, respuesta.getStatusCode().value());
        verifyNoInteractions(logErrorService);
    }

    @Test
    void excepcionInesperadaSeRegistraComoExcepcionNoControlada() {
        ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones(logErrorService);
        MockHttpServletRequest solicitud = new MockHttpServletRequest("POST", "/api/v1/evaluaciones");

        ResponseEntity<com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse<Void>> respuesta =
                manejador.manejarErrorInesperado(new IllegalStateException("boom"), solicitud);

        assertEquals(500, respuesta.getStatusCode().value());
        verify(logErrorService).registrarError(any(), eq(TipoError.EXCEPCION_NO_CONTROLADA), eq("boom"), any(),
                eq("/api/v1/evaluaciones"), eq("POST"), eq((short) 500), isNull());
    }

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
