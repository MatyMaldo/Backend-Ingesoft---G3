package com.educoders.Seguridad_Auditoria.presentacion.controladores;

import com.educoders.Seguridad_Auditoria.seguridad.JwtAuthenticationFilter;
import com.educoders.Seguridad_Auditoria.servicio.AutenticacionService;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import com.educoders.Seguridad_Auditoria.servicio.RestablecimientoPasswordService;
import com.educoders.config.SeguridadConfig;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutenticacionController.class)
@Import(SeguridadConfig.class)
@TestPropertySource(properties = "server.port=0")
class RestablecimientoPasswordControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean private AutenticacionService autenticacionService;
    @MockBean private RestablecimientoPasswordService restablecimientoPasswordService;
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
    void solicitarCodigoEsPublico() throws Exception {
        mvc.perform(post("/api/v1/auth/password/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@colegio.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.mensaje").exists());

        verify(restablecimientoPasswordService).solicitarCodigo("ana@colegio.com");
    }

    @Test
    void verificarCodigoEsPublico() throws Exception {
        mvc.perform(post("/api/v1/auth/password/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@colegio.com\",\"codigo\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.valido").value(false));
    }

    @Test
    void restablecerPasswordEsPublico() throws Exception {
        mvc.perform(post("/api/v1/auth/password/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@colegio.com\",\"codigo\":\"123456\","
                                + "\"nuevaPassword\":\"NuevaClave123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.mensaje").value(
                        "Contraseña actualizada correctamente."));
    }

    @Test
    void correoInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/v1/auth/password/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest());
    }
}
