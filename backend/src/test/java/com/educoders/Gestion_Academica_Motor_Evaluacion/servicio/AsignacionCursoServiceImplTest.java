package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.CursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearAsignacionCursoRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.AsignacionCursoResponse;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del servicio de asignación curso-docente-sección.
 * Los IDs de prueba se basan en los datos de Scripts_Inserts_EduCoders_v2.sql:
 *  - Docente ID 2: Juan Perez (Matematicas), activo
 *  - Docente ID 3: Maria Gomez (Comunicacion), activa
 *  - Seccion ID 1: 1A, grado 1, anio 2027
 *  - Curso ID 1: Matematicas (ya asignado a docente 2 en seccion 1)
 *  - Curso ID 2: Comunicacion (ya asignado a docente 3 en seccion 1)
 *  - Curso ID 3: Ciencias Sociales (sin asignacion en seccion 1)
 */
@ExtendWith(MockitoExtension.class)
class AsignacionCursoServiceImplTest {

    @Mock
    private AsignacionCursoRepository asignacionRepo;

    @Mock
    private CursoRepository cursoRepo;

    @Mock
    private DocenteRepository docenteRepo;

    @Mock
    private SeccionRepository seccionRepo;

    @Mock
    private PeriodoMatriculaRepository periodoRepo;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private AsignacionCursoServiceImpl asignacionService;

    private Curso crearCurso(Long id, String nombre) {
        Curso c = new Curso();
        c.setId(id);
        c.setNombre(nombre);
        return c;
    }

    private Docente crearDocente(Long id, String nombre, String apellido, boolean activo) {
        Docente d = new Docente();
        d.setId(id);
        d.setNombre(nombre);
        d.setApellidoPaterno(apellido);
        d.setActivo(activo);
        return d;
    }

    private Seccion crearSeccion(Long id, String nombre, Long gradoId, String gradoNombre, Integer anio) {
        Seccion s = new Seccion();
        s.setId(id);
        s.setNombre(nombre);
        com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado g =
                new com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado();
        g.setId(gradoId);
        g.setNombre(gradoNombre);
        s.setGrado(g);
        com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico a =
                new com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico();
        a.setId(1L);
        a.setAnio(anio);
        s.setAnioAcademico(a);
        return s;
    }

    @BeforeEach
    void setUp() {
        PeriodoMatricula periodoVigente = new PeriodoMatricula();
        periodoVigente.setId(1L);
        periodoVigente.setFechaInicio(java.time.LocalDate.now().minusMonths(1));
        periodoVigente.setFechaFin(java.time.LocalDate.now().plusMonths(3));
        com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico anio =
                new com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico();
        anio.setId(1L);
        periodoVigente.setAnioAcademico(anio);

        lenient().when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(periodoVigente));
    }

    @Test
    void crearAsignacion_exitosa() {
        // Arrange: curso 3 (Ciencias Sociales) no tiene asignacion en seccion 1
        Curso curso = crearCurso(3L, "Ciencias Sociales");
        Docente docente = crearDocente(2L, "Juan", "Perez", true);
        Seccion seccion = crearSeccion(1L, "A", 1L, "1er Grado", 2027);

        when(cursoRepo.findById(3L)).thenReturn(Optional.of(curso));
        when(docenteRepo.findById(2L)).thenReturn(Optional.of(docente));
        when(seccionRepo.findById(1L)).thenReturn(Optional.of(seccion));
        when(asignacionRepo.existsByCursoIdAndSeccionId(3L, 1L)).thenReturn(false);
        when(asignacionRepo.save(any(AsignacionCurso.class))).thenAnswer(inv -> {
            AsignacionCurso a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        CrearAsignacionCursoRequest request = new CrearAsignacionCursoRequest();
        request.setCursoId(3L);
        request.setDocenteId(2L);
        request.setSeccionId(1L);

        // Act
        AsignacionCursoResponse resultado = asignacionService.crear(request);

        // Assert
        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals(3L, resultado.getCursoId());
        assertEquals("Ciencias Sociales", resultado.getCursoNombre());
        assertEquals(2L, resultado.getDocenteId());
        assertEquals("Juan Perez", resultado.getDocenteNombre());
        assertEquals(1L, resultado.getSeccionId());
        assertEquals("A", resultado.getSeccionNombre());
        assertEquals(1L, resultado.getGradoId());
        assertEquals("1er Grado", resultado.getGradoNombre());
        assertEquals(2027, resultado.getAnio());

        verify(asignacionRepo).save(any(AsignacionCurso.class));
    }

    @Test
    void crearAsignacion_programacionCerrada_sinPeriodo() {
        Curso curso = crearCurso(3L, "Ciencias Sociales");
        Docente docente = crearDocente(2L, "Juan", "Perez", true);
        Seccion seccion = crearSeccion(1L, "A", 1L, "1er Grado", 2027);

        when(cursoRepo.findById(3L)).thenReturn(Optional.of(curso));
        when(docenteRepo.findById(2L)).thenReturn(Optional.of(docente));
        when(seccionRepo.findById(1L)).thenReturn(Optional.of(seccion));
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.empty());

        CrearAsignacionCursoRequest request = new CrearAsignacionCursoRequest();
        request.setCursoId(3L);
        request.setDocenteId(2L);
        request.setSeccionId(1L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> asignacionService.crear(request));
        assertEquals("La programación de horarios está cerrada: solo se habilita mientras el año académico está en preparación.", ex.getMessage());

        verify(asignacionRepo, never()).save(any(AsignacionCurso.class));
    }

    @Test
    void crearAsignacion_conflicto_cursoYaAsignado() {
        // Arrange: curso 1 (Matematicas) ya esta asignado a docente 2 en seccion 1
        Curso curso = crearCurso(1L, "Matematicas");
        Docente docente = crearDocente(3L, "Maria", "Gomez", true);
        Seccion seccion = crearSeccion(1L, "A", 1L, "1er Grado", 2027);

        when(cursoRepo.findById(1L)).thenReturn(Optional.of(curso));
        when(docenteRepo.findById(3L)).thenReturn(Optional.of(docente));
        when(seccionRepo.findById(1L)).thenReturn(Optional.of(seccion));
        when(asignacionRepo.existsByCursoIdAndSeccionId(1L, 1L)).thenReturn(true);

        CrearAsignacionCursoRequest request = new CrearAsignacionCursoRequest();
        request.setCursoId(1L);
        request.setDocenteId(3L);
        request.setSeccionId(1L);

        // Act & Assert
        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> asignacionService.crear(request));
        assertEquals("El curso ya está asignado a otro docente en esta sección.", ex.getMessage());

        verify(asignacionRepo, never()).save(any(AsignacionCurso.class));
    }

    @Test
    void crearAsignacion_docenteInactivo() {
        Curso curso = crearCurso(3L, "Ciencias Sociales");
        Docente docente = crearDocente(2L, "Juan", "Perez", false); // inactivo

        when(cursoRepo.findById(3L)).thenReturn(Optional.of(curso));
        when(docenteRepo.findById(2L)).thenReturn(Optional.of(docente));

        CrearAsignacionCursoRequest request = new CrearAsignacionCursoRequest();
        request.setCursoId(3L);
        request.setDocenteId(2L);
        request.setSeccionId(1L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> asignacionService.crear(request));
        assertEquals("El docente no está activo.", ex.getMessage());

        verify(asignacionRepo, never()).save(any(AsignacionCurso.class));
    }

    @Test
    void crearAsignacion_cursoNoExiste() {
        when(cursoRepo.findById(999L)).thenReturn(Optional.empty());

        CrearAsignacionCursoRequest request = new CrearAsignacionCursoRequest();
        request.setCursoId(999L);
        request.setDocenteId(2L);
        request.setSeccionId(1L);

        assertThrows(RecursoNoEncontradoException.class,
                () -> asignacionService.crear(request));

        verify(asignacionRepo, never()).save(any(AsignacionCurso.class));
    }

    @Test
    void crearAsignacion_seccionNoExiste() {
        Curso curso = crearCurso(3L, "Ciencias Sociales");
        Docente docente = crearDocente(2L, "Juan", "Perez", true);

        when(cursoRepo.findById(3L)).thenReturn(Optional.of(curso));
        when(docenteRepo.findById(2L)).thenReturn(Optional.of(docente));
        when(seccionRepo.findById(999L)).thenReturn(Optional.empty());

        CrearAsignacionCursoRequest request = new CrearAsignacionCursoRequest();
        request.setCursoId(3L);
        request.setDocenteId(2L);
        request.setSeccionId(999L);

        assertThrows(RecursoNoEncontradoException.class,
                () -> asignacionService.crear(request));

        verify(asignacionRepo, never()).save(any(AsignacionCurso.class));
    }

    @Test
    void crearAsignacion_docenteNoExiste() {
        when(cursoRepo.findById(3L)).thenReturn(Optional.of(crearCurso(3L, "Ciencias Sociales")));
        when(docenteRepo.findById(999L)).thenReturn(Optional.empty());

        CrearAsignacionCursoRequest request = new CrearAsignacionCursoRequest();
        request.setCursoId(3L);
        request.setDocenteId(999L);
        request.setSeccionId(1L);

        assertThrows(RecursoNoEncontradoException.class,
                () -> asignacionService.crear(request));

        verify(asignacionRepo, never()).save(any(AsignacionCurso.class));
    }
}