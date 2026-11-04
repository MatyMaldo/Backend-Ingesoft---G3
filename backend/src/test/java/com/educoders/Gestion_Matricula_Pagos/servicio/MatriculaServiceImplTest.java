package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarMatriculaRequest;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceImplTest {

    @Mock private MatriculaRepository matriculaRepository;
    @Mock private PeriodoMatriculaRepository periodoMatriculaRepository;
    @Mock private SeccionRepository seccionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private MatriculaServiceImpl service;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Administrador crearAdmin() {
        Administrador a = new Administrador();
        a.setId(1L);
        return a;
    }

    private Apoderado crearApoderado() {
        Apoderado a = new Apoderado();
        a.setId(4L);
        return a;
    }

    private Estudiante crearEstudiante() {
        Estudiante e = new Estudiante();
        e.setId(5L);
        e.setNombre("Luis");
        e.setApellidoPaterno("Ramirez");
        e.setApellidoMaterno("Torres");
        e.setCodigo("EST-001");
        e.setActivo(true);
        return e;
    }

    private Seccion crearSeccion() {
        AnioAcademico anio = new AnioAcademico();
        anio.setId(1L);
        anio.setAnio(2027);
        Grado g = new Grado();
        g.setId(1L);
        g.setNombre("1er Grado");
        Seccion s = new Seccion();
        s.setId(1L);
        s.setNombre("A");
        s.setCapacidadMaxima(30);
        s.setGrado(g);
        s.setAnioAcademico(anio);
        return s;
    }

    private PeriodoMatricula crearPeriodo(boolean vigente) {
        PeriodoMatricula p = new PeriodoMatricula();
        p.setFechaInicio(vigente ? LocalDate.now().minusDays(10) : LocalDate.now().minusDays(60));
        p.setFechaFin(vigente ? LocalDate.now().plusDays(20) : LocalDate.now().minusDays(1));
        return p;
    }

    private void autenticar(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    @Test
    void registrarMatricula_exitoso() {
        autenticar(crearAdmin());
        Seccion seccion = crearSeccion();
        Estudiante estudiante = crearEstudiante();

        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodo(true)));
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoIn(anyLong(), anyLong(), anyCollection()))
                .thenReturn(false);
        when(matriculaRepository.countBySeccionIdAndEstadoIn(anyLong(), anyCollection())).thenReturn(0L);
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(inv -> {
            Matricula m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        MatriculaResponse r = service.registrarMatricula(new RegistrarMatriculaRequest(5L, 1L));

        assertEquals(100L, r.id());
        assertEquals(EstadoMatricula.PENDIENTE, r.estado());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void registrarMatricula_fueraDePeriodo_rechaza() {
        autenticar(crearAdmin());
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(crearSeccion()));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodo(false)));

        assertThrows(ReglaNegocioException.class,
                () -> service.registrarMatricula(new RegistrarMatriculaRequest(5L, 1L)));

        verify(matriculaRepository, never()).save(any(Matricula.class));
    }

    @Test
    void registrarMatricula_rolNoAdmin_rechaza() {
        autenticar(crearApoderado());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.registrarMatricula(new RegistrarMatriculaRequest(5L, 1L)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}