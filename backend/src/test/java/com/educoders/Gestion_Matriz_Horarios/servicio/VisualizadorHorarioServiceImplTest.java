package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import java.time.LocalTime;


@ExtendWith(MockitoExtension.class)
class VisualizadorHorarioServiceImplTest {
    @Mock AnioAcademicoRepository anioRepo;
    @Mock SeccionRepository seccionRepo;
    @Mock BloqueHorarioRepository bloqueRepo;
    @Mock MatriculaRepository matriculaRepo;
    @Mock EstudianteRepository estudianteRepo;
    @InjectMocks VisualizadorHorarioServiceImpl service;

    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    private void autenticar(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    private AnioAcademico anioActivo() {
        AnioAcademico a = new AnioAcademico();
        a.setId(8L);
        a.setAnio(LocalDate.now().getYear());
        when(anioRepo.findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(any(), any()))
                .thenReturn(Optional.of(a));
        return a;
    }

    private Matricula matricula(AnioAcademico anio, EstadoMatricula estado) {
        Seccion seccion = new Seccion();
        seccion.setId(14L);
        seccion.setAnioAcademico(anio);
        Matricula matricula = new Matricula();
        matricula.setSeccion(seccion);
        matricula.setEstado(estado);
        return matricula;
    }

    @Test void adminConsultaSeccionSinBloques() {
        Administrador admin = new Administrador(); admin.setId(1L); autenticar(admin);
        AnioAcademico a = anioActivo();
        Seccion s = new Seccion(); s.setId(14L); s.setAnioAcademico(a);
        when(seccionRepo.findById(14L)).thenReturn(Optional.of(s));
        when(bloqueRepo.findByAsignacionCursoSeccionId(14L)).thenReturn(List.of());
        var respuesta = service.porSeccion(14L);
        assertEquals("SIN_BLOQUES", respuesta.estado());
        assertEquals("Aún no se ha registrado un horario para esta sección", respuesta.mensaje());
    }

    @Test void adminNoPuedeConsultarSeccionHistorica() {
        Administrador admin = new Administrador(); admin.setId(1L); autenticar(admin);
        AnioAcademico a = anioActivo();
        AnioAcademico historico = new AnioAcademico(); historico.setId(7L);
        Seccion s = new Seccion(); s.setId(14L); s.setAnioAcademico(historico);
        when(seccionRepo.findById(14L)).thenReturn(Optional.of(s));
        assertThrows(ReglaNegocioException.class, () -> service.porSeccion(14L));
        verify(bloqueRepo, never()).findByAsignacionCursoSeccionId(any());
    }

    @Test void adminConsultaDocenteSinCarga() {
        Administrador admin = new Administrador(); admin.setId(1L); autenticar(admin);
        anioActivo();
        when(bloqueRepo.findByAsignacionCursoDocenteIdAndAsignacionCursoSeccionAnioAcademicoId(9L,8L))
                .thenReturn(List.of());
        assertEquals("No tiene carga horaria asignada", service.porDocente(9L).mensaje());
    }

    @Test void docenteSoloConsultaSuIdentidad() {
        Docente docente = new Docente(); docente.setId(9L); autenticar(docente);
        anioActivo();
        when(bloqueRepo.findByAsignacionCursoDocenteIdAndAsignacionCursoSeccionAnioAcademicoId(9L,8L))
                .thenReturn(List.of());
        assertEquals("SIN_BLOQUES", service.miHorarioDocente().estado());
        verify(bloqueRepo).findByAsignacionCursoDocenteIdAndAsignacionCursoSeccionAnioAcademicoId(9L,8L);
    }

    @Test void estudianteConMatriculaPendienteNoVeBloques() {
        Estudiante e = new Estudiante(); e.setId(5L); autenticar(e);
        AnioAcademico a = anioActivo();
        when(matriculaRepo.buscarMatriculasConSeccionYAnio(5L))
                .thenReturn(List.of(matricula(a, EstadoMatricula.PENDIENTE)));
        assertEquals("SIN_MATRICULA", service.miHorarioEstudiante().estado());
        verifyNoInteractions(bloqueRepo);
    }

    @Test void estudianteConMatriculaConfirmadaYSinBloques() {
        Estudiante e = new Estudiante(); e.setId(5L); autenticar(e);
        AnioAcademico a = anioActivo();
        when(matriculaRepo.buscarMatriculasConSeccionYAnio(5L))
                .thenReturn(List.of(matricula(a, EstadoMatricula.CONFIRMADA)));
        when(bloqueRepo.findByAsignacionCursoSeccionId(14L)).thenReturn(List.of());
        assertEquals("SIN_BLOQUES", service.miHorarioEstudiante().estado());
    }

    @Test void estudianteConMatriculaHistoricaNoVeBloques() {
        Estudiante e = new Estudiante(); e.setId(5L); autenticar(e);
        anioActivo();
        AnioAcademico pasado = new AnioAcademico(); pasado.setId(7L);
        when(matriculaRepo.buscarMatriculasConSeccionYAnio(5L))
                .thenReturn(List.of(matricula(pasado, EstadoMatricula.CONFIRMADA)));
        assertEquals("SIN_MATRICULA", service.miHorarioEstudiante().estado());
        verifyNoInteractions(bloqueRepo);
    }

    @Test void apoderadoNoVeEstudianteAjeno() {
        Apoderado a = new Apoderado(); a.setId(3L); autenticar(a);
        Apoderado otro = new Apoderado(); otro.setId(4L);
        Estudiante e = new Estudiante(); e.setId(5L); e.setApoderado(otro);
        when(estudianteRepo.findById(5L)).thenReturn(Optional.of(e));
        var exception = assertThrows(ResponseStatusException.class, () -> service.porApoderado(5L));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(matriculaRepo,bloqueRepo);
    }

    @Test void apoderadoVeSoloEstudianteVinculadoConMatriculaConfirmada() {
        Apoderado a = new Apoderado(); a.setId(3L); autenticar(a);
        Estudiante e = new Estudiante(); e.setId(5L); e.setApoderado(a);
        when(estudianteRepo.findById(5L)).thenReturn(Optional.of(e));
        AnioAcademico anio = anioActivo();
        when(matriculaRepo.buscarMatriculasConSeccionYAnio(5L))
                .thenReturn(List.of(matricula(anio, EstadoMatricula.CONFIRMADA)));
        when(bloqueRepo.findByAsignacionCursoSeccionId(14L)).thenReturn(List.of());
        assertEquals("SIN_BLOQUES", service.porApoderado(5L).estado());
    }

    @Test void unDocenteNoPuedeConsultarHorarioAdministrativo() {
        Docente d = new Docente(); d.setId(9L); autenticar(d);
        var e = assertThrows(ResponseStatusException.class, () -> service.porSeccion(14L));
        assertEquals(HttpStatus.FORBIDDEN,e.getStatusCode());
        verifyNoInteractions(anioRepo, bloqueRepo);
    }

    @Test void sinAutenticacionEs401() {
        var e = assertThrows(ResponseStatusException.class, service::miHorarioDocente);
        assertEquals(HttpStatus.UNAUTHORIZED,e.getStatusCode());
    }


    @Test
    void adminConsultaSeccionConBloqueHorario() {
        Administrador admin = new Administrador();
        admin.setId(1L);
        autenticar(admin);

        AnioAcademico anio = anioActivo();

        Grado grado = new Grado();
        grado.setId(1L);
        grado.setNombre("Primero");

        Seccion seccion = new Seccion();
        seccion.setId(14L);
        seccion.setNombre("A");
        seccion.setGrado(grado);
        seccion.setAnioAcademico(anio);

        Curso curso = new Curso();
        curso.setId(1L);
        curso.setNombre("Matemáticas");

        Docente docente = new Docente();
        docente.setId(9L);
        docente.setNombre("Juan");
        docente.setApellidoPaterno("Perez");

        AsignacionCurso asignacion = new AsignacionCurso();
        asignacion.setId(20L);
        asignacion.setCurso(curso);
        asignacion.setDocente(docente);
        asignacion.setSeccion(seccion);

        Aula aula = new Aula();
        aula.setId(1L);
        aula.setNombre("Aula 101");

        BloqueHorario bloque = new BloqueHorario();
        bloque.setId(100L);
        bloque.setDiaSemana(DiaSemana.LUNES);
        bloque.setHoraInicio(LocalTime.of(8, 0));
        bloque.setHoraFin(LocalTime.of(10, 0));
        bloque.setAula(aula);
        bloque.setAsignacionCurso(asignacion);

        when(seccionRepo.findById(14L))
                .thenReturn(Optional.of(seccion));

        when(bloqueRepo.findByAsignacionCursoSeccionId(14L))
                .thenReturn(List.of(bloque));

        var respuesta = service.porSeccion(14L);

        assertEquals("OK", respuesta.estado());
        assertEquals(1, respuesta.bloques().size());

        var resultado = respuesta.bloques().get(0);

        assertEquals("Matemáticas", resultado.getCursoNombre());
        assertEquals("Juan Perez", resultado.getDocenteNombre());
        assertEquals("Aula 101", resultado.getAulaNombre());
        assertEquals(DiaSemana.LUNES, resultado.getDiaSemana());
        assertEquals(LocalTime.of(8, 0), resultado.getHoraInicio());
        assertEquals(LocalTime.of(10, 0), resultado.getHoraFin());
    }

}
