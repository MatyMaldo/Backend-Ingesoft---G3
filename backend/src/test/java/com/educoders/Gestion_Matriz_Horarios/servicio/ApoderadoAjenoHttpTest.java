package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.presentacion.controladores.VisualizadorHorarioController;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApoderadoAjenoHttpTest {

    @Mock AnioAcademicoRepository anioRepo;
    @Mock SeccionRepository seccionRepo;
    @Mock BloqueHorarioRepository bloqueRepo;
    @Mock MatriculaRepository matriculaRepo;
    @Mock EstudianteRepository estudianteRepo;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void apoderadoNoPuedeConsultarEstudianteAjenoPorHttp() throws Exception {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(3L);

        Apoderado otro = new Apoderado();
        otro.setId(4L);

        Estudiante estudiante = new Estudiante();
        estudiante.setId(5L);
        estudiante.setApoderado(otro);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                apoderado, null, List.of()
            )
        );

        when(estudianteRepo.findById(5L))
            .thenReturn(Optional.of(estudiante));

        var service = new VisualizadorHorarioServiceImpl(
            anioRepo, seccionRepo, bloqueRepo,
            matriculaRepo, estudianteRepo
        );

        var controller = new VisualizadorHorarioController(
            service, estudianteRepo
        );

        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/api/v1/visualizadores-horario/apoderado/5"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(matriculaRepo, bloqueRepo);
    }
}