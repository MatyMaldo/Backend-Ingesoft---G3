package com.educoders.config;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores.AsignacionCursoController;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.AsignacionCursoService;
import com.educoders.Gestion_Matriz_Horarios.presentacion.controladores.BloqueHorarioController;
import com.educoders.Gestion_Matriz_Horarios.presentacion.controladores.SesionClaseController;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.SesionClaseResponse;
import com.educoders.Gestion_Matriz_Horarios.servicio.BloqueHorarioService;
import com.educoders.Gestion_Matriz_Horarios.servicio.HorarioCargaMasivaService;
import com.educoders.Gestion_Matriz_Horarios.servicio.HorarioCsvService;
import com.educoders.Gestion_Matriz_Horarios.servicio.SesionClaseService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AsignacionCursoController.class,
        BloqueHorarioController.class,
        SesionClaseController.class
})
@Import(SeguridadConfig.class)
@TestPropertySource(properties = "server.port=0")
class SeguridadEscrituraHorariosTest {

    @Autowired
    private MockMvc mvc;

    @MockBean private AsignacionCursoService asignacionCursoService;
    @MockBean private BloqueHorarioService bloqueHorarioService;
    @MockBean private HorarioCsvService horarioCsvService;
    @MockBean private HorarioCargaMasivaService horarioCargaMasivaService;
    @MockBean private SesionClaseService sesionClaseService;
    @MockBean private OperacionMasivaService operacionMasivaService;
    @MockBean private UsuarioAutenticadoResolver usuarioAutenticadoResolver;
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

    private static final String ASIGNACION = """
            {"cursoId":1,"docenteId":1,"seccionId":1}
            """;
    private static final String BLOQUE = """
            {"asignacionCursoId":1,"aulaId":1,"diaSemana":"LUNES",
             "horaInicio":"08:00","horaFin":"09:00"}
            """;
    private static final String PREVIEW = """
            {"anioAcademicoId":1,"seccionId":1,
             "fechaInicio":"2027-03-02","fechaFin":"2027-03-06"}
            """;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminPuedeCambiarOmisionDeSesion() throws Exception {
        when(sesionClaseService.cambiarOmision(1L, true))
                .thenReturn(new SesionClaseResponse());

        mvc.perform(patch("/api/v1/sesiones-clase/1/omision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"omitida\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DOCENTE")
    void docenteNoPuedeCambiarOmisionDeSesion() throws Exception {
        mvc.perform(patch("/api/v1/sesiones-clase/1/omision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"omitida\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminPuedeCrearAsignacion() throws Exception {
        mvc.perform(post("/api/v1/asignaciones-curso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ASIGNACION))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DOCENTE")
    void docenteNoPuedeCrearAsignacion() throws Exception {
        mvc.perform(post("/api/v1/asignaciones-curso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ASIGNACION))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminPuedeCrearBloque() throws Exception {
        mvc.perform(post("/api/v1/bloques-horario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BLOQUE))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void estudianteNoPuedeCrearBloque() throws Exception {
        mvc.perform(post("/api/v1/bloques-horario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BLOQUE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminPuedeGuardarBloquesMasivamente() throws Exception {
        mvc.perform(put("/api/v1/bloques-horario/seccion/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"anioAcademicoId":1,"bloques":[]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DOCENTE")
    void docenteNoPuedeGuardarBloquesMasivamente() throws Exception {
        mvc.perform(put("/api/v1/bloques-horario/seccion/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"anioAcademicoId":1,"bloques":[]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminPuedePrevisualizarSesiones() throws Exception {
        mvc.perform(post("/api/v1/sesiones-clase/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PREVIEW))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "APODERADO")
    void apoderadoNoPuedeGenerarSesiones() throws Exception {
        mvc.perform(post("/api/v1/sesiones-clase/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PREVIEW))
                .andExpect(status().isForbidden());
    }
}
