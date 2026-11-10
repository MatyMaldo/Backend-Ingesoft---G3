package com.educoders.config;

import com.educoders.Gestion_Matriz_Horarios.presentacion.controladores.SesionClaseController;
import com.educoders.Gestion_Matriz_Horarios.servicio.SesionClaseService;
import com.educoders.Infraestructura_Servicios.operaciones.OperacionMasivaService;
import com.educoders.Infraestructura_Servicios.operaciones.UsuarioAutenticadoResolver;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.seguridad.JwtAuthenticationFilter;
import com.educoders.Seguridad_Auditoria.servicio.JwtService;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;

import io.jsonwebtoken.ExpiredJwtException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SesionClaseController.class)
@Import({SeguridadConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "server.port=0")
class JwtVencidoHttpTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SesionClaseService service;

    @MockBean
    private OperacionMasivaService operacionMasivaService;

    @MockBean
    private UsuarioAutenticadoResolver usuarioAutenticadoResolver;

    @MockBean
    private LogErrorService logErrorService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    void jwtVencidoDevuelve401() throws Exception {
        when(jwtService.validarToken("token-vencido"))
            .thenThrow(new ExpiredJwtException(
                null, null, "JWT vencido"
            ));

        mvc.perform(get("/api/v1/sesiones-clase")
                .header("Authorization", "Bearer token-vencido"))
            .andExpect(status().isUnauthorized());
    }
}