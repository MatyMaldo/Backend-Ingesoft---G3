package com.educoders.config;

import com.educoders.Gestion_Matriz_Horarios.presentacion.controladores.SesionClaseController;
import com.educoders.Gestion_Matriz_Horarios.servicio.SesionClaseService;
import com.educoders.Seguridad_Auditoria.seguridad.JwtAuthenticationFilter;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SesionClaseController.class)
@Import(SeguridadConfig.class)
class SeguridadHorariosTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SesionClaseService service;

    @MockBean
    private LogErrorService logErrorService;

    @MockBean
    private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void configurarFiltro() throws Exception {
        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeConsultarSeccion() throws Exception {
        mvc.perform(get("/api/v1/sesiones-clase")
                .param("seccionId", "1")
                .param("fechaInicio", "2027-03-01")
                .param("fechaFin", "2027-03-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void estudianteNoPuedeConsultarSeccion() throws Exception {
        mvc.perform(get("/api/v1/sesiones-clase")
                .param("seccionId", "1")
                .param("fechaInicio", "2027-03-01")
                .param("fechaFin", "2027-03-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DOCENTE")
    void docenteNoPuedeConsultarPorId() throws Exception {
        mvc.perform(get("/api/v1/sesiones-clase/docente/5")
                .param("fechaInicio", "2027-03-01")
                .param("fechaFin", "2027-03-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DOCENTE")
    void docentePuedeAccederAMiHorario() throws Exception {
        mvc.perform(get("/api/v1/sesiones-clase/mi-horario/docente")
                .param("fechaInicio", "2027-03-01")
                .param("fechaFin", "2027-03-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void estudiantePuedeAccederAMiHorario() throws Exception {
        mvc.perform(get("/api/v1/sesiones-clase/mi-horario/estudiante")
                .param("fechaInicio", "2027-03-01")
                .param("fechaFin", "2027-03-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "APODERADO")
    void apoderadoPuedeAccederARutaAutorizada() throws Exception {
        mvc.perform(get("/api/v1/sesiones-clase/apoderado/5")
                .param("fechaInicio", "2027-03-01")
                .param("fechaFin", "2027-03-31"))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioSinAutenticacionRecibe401() throws Exception {
        mvc.perform(get("/api/v1/sesiones-clase")
                .param("seccionId", "1")
                .param("fechaInicio", "2027-03-01")
                .param("fechaFin", "2027-03-31"))
                .andExpect(status().isUnauthorized());
    }
}
