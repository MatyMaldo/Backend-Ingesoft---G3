package com.educoders.config;

import com.educoders.Gestion_Matricula_Pagos.presentacion.controladores.MatriculaController;
import com.educoders.Gestion_Matricula_Pagos.servicio.MatriculaCargaMasivaService;
import com.educoders.Gestion_Matricula_Pagos.servicio.MatriculaService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatriculaController.class)
@Import(SeguridadConfig.class)
@TestPropertySource(properties = "server.port=0")
class SeguridadMatriculasTest {

    @Autowired
    private MockMvc mvc;

    @MockBean private MatriculaService matriculaService;
    @MockBean private MatriculaCargaMasivaService cargaMasivaService;
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
    @WithMockUser(roles = "ESTUDIANTE")
    void estudianteNoPuedeEditarMatricula() throws Exception {
        mvc.perform(put("/api/v1/matriculas/1")
                        .contentType("application/json")
                        .content("{\"seccionId\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DOCENTE")
    void docenteNoPuedeEliminarMatricula() throws Exception {
        mvc.perform(delete("/api/v1/matriculas/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "APODERADO")
    void apoderadoNoPuedeConfirmarCargaMasiva() throws Exception {
        mvc.perform(post("/api/v1/matriculas/carga-masiva/confirmar")
                        .contentType("application/json")
                        .content("{\"anioAcademicoId\":1,\"filas\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void estudianteNoPuedeDescargarPlantilla() throws Exception {
        mvc.perform(get("/api/v1/matriculas/plantilla"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeDescargarPlantilla() throws Exception {
        when(cargaMasivaService.generarPlantilla("csv")).thenReturn(new byte[]{1, 2, 3});

        mvc.perform(get("/api/v1/matriculas/plantilla"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeExportar() throws Exception {
        when(cargaMasivaService.exportar(1L, "csv")).thenReturn(new byte[]{1, 2, 3});

        mvc.perform(get("/api/v1/matriculas/exportar").param("anioId", "1"))
                .andExpect(status().isOk());
    }
}
