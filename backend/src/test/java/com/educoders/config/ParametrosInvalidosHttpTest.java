package com.educoders.config;

import com.educoders.Gestion_Matriz_Horarios.presentacion.controladores.SesionClaseController;
import com.educoders.Gestion_Matriz_Horarios.servicio.SesionClaseService;
import com.educoders.Infraestructura_Servicios.exception.ManejadorGlobalExcepciones;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasivaService;
import com.educoders.Infraestructura_Servicios.operaciones.UsuarioAutenticadoResolver;
import com.educoders.Seguridad_Auditoria.seguridad.JwtAuthenticationFilter;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un parámetro ausente o con un valor del tipo equivocado es un error de quien llama: responde 400 y no se registra
 * en el log de errores (que solo admite excepciones no controladas, procesos automáticos y correos fallidos).
 */
@WebMvcTest(SesionClaseController.class)
@Import({SeguridadConfig.class, ManejadorGlobalExcepciones.class})
@TestPropertySource(properties = "server.port=0")
class ParametrosInvalidosHttpTest {

    @Autowired MockMvc mvc;

    @MockBean SesionClaseService service;
    @MockBean OperacionMasivaService operacionMasivaService;
    @MockBean UsuarioAutenticadoResolver usuarioAutenticadoResolver;
    @MockBean LogErrorService logErrorService;
    @MockBean JwtAuthenticationFilter jwtFilter;

    /** El filtro JWT real borra la autenticación de prueba: se sustituye por uno que deja pasar la petición. */
    @BeforeEach
    void dejarPasarElFiltro() throws Exception {
        doAnswer(inv -> {
            FilterChain cadena = inv.getArgument(2);
            cadena.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void parametroObligatorioAusenteResponde400SinRegistrarEnElLog() throws Exception {
        mvc.perform(get("/api/v1/sesiones-clase"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Falta el parámetro obligatorio 'seccionId'."));

        verify(logErrorService, never()).registrarError(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void fechaConFormatoInvalidoResponde400SinRegistrarEnElLog() throws Exception {
        mvc.perform(get("/api/v1/sesiones-clase")
                        .param("seccionId", "3")
                        .param("fechaInicio", "no-es-una-fecha")
                        .param("fechaFin", "2026-03-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El valor del parámetro 'fechaInicio' no es válido."));

        verify(logErrorService, never()).registrarError(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
