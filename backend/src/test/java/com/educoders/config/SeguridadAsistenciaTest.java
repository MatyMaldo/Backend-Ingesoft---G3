package com.educoders.config;

import com.educoders.Gestion_Asistencia.presentacion.controladores.AsistenciaController;
import com.educoders.Gestion_Asistencia.servicio.AsistenciaService;
import com.educoders.Seguridad_Auditoria.seguridad.JwtAuthenticationFilter;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsistenciaController.class)
@Import(SeguridadConfig.class)
@TestPropertySource(properties = "server.port=0")
class SeguridadAsistenciaTest {

    @Autowired
    private MockMvc mvc;

    @MockBean private AsistenciaService asistenciaService;
    @MockBean private LogErrorService logErrorService;
    @MockBean private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void configurarFiltro() throws Exception {
        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "DOCENTE")
    void docentePuedeConsultarDetalle() throws Exception {
        mvc.perform(get("/api/v1/asistencias/sesiones/1"))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ESTUDIANTE")
    void estudianteNoPuedeConsultarDetalle() throws Exception {
        mvc.perform(get("/api/v1/asistencias/sesiones/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "DOCENTE")
    void docentePuedeConsultarSuHistorial() throws Exception {
        mvc.perform(get("/api/v1/asistencias/docente/mi-asistencia"))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ESTUDIANTE")
    void estudiantePuedeConsultarSuHistorial() throws Exception {
        mvc.perform(get("/api/v1/asistencias/estudiante/mi-asistencia"))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "APODERADO")
    void apoderadoPuedeConsultarHistorialDeEstudiante() throws Exception {
        mvc.perform(get("/api/v1/asistencias/apoderado/10"))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "DOCENTE")
    void docentePuedeRegistrarSuAsistencia() throws Exception {
        mvc.perform(put("/api/v1/asistencias/sesiones/1/docente"))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ESTUDIANTE")
    void estudianteNoPuedeRegistrarAsistenciaDocente() throws Exception {
        mvc.perform(put("/api/v1/asistencias/sesiones/1/docente"))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ADMINISTRADOR")
    void administradorNoTieneAccesoInicial() throws Exception {
        mvc.perform(get("/api/v1/asistencias/sesiones/1"))
                .andExpect(status().isForbidden());
    }
}
