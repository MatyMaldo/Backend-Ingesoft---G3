package com.educoders.Seguridad_Auditoria.presentacion.controladores;

import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogErrorControllerTest {

    @Test
    void ordenaSiemprePorFechaHora() {
        LogErrorService service = mock(LogErrorService.class);
        when(service.listarErrores(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        LogErrorController controller = new LogErrorController(service);

        var response = controller.listarErrores(null, null, null, null, 0, 15, "ASC");

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listarErrores(isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture());

        Sort.Order orden = pageableCaptor.getValue().getSort().getOrderFor("fechaHora");
        assertNotNull(orden);
        assertTrue(orden.isAscending());
    }
}
