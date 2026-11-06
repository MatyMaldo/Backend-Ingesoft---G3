package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.persistencia.AulaRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.CrearBloqueRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ActualizarBloqueRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.BloqueHorarioResponse;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
 * Pruebas unitarias del servicio de bloques horario.
 * Datos basados en Scripts_Inserts_EduCoders_v2.sql:
 *  - Aula ID 1: 'Aula 101', capacidad 30
 *  - Seccion ID 1: 1A, grado 1, anio 2027 (anioAcademico ID 1)
 *  - AsignacionCurso ID 1: curso 1 (Matematicas), docente 2 (Juan), seccion 1
 *  - AsignacionCurso ID 2: curso 2 (Comunicacion), docente 3 (Maria), seccion 1
 *  - PeriodoMatricula ID 1: anio 1, fecha_inicio 2026-10-01, fecha_fin 2027-03-01
 */
@ExtendWith(MockitoExtension.class)
class BloqueHorarioServiceImplTest {

    @Mock
    private BloqueHorarioRepository bloqueRepo;

    @Mock
    private AsignacionCursoRepository asignacionRepo;

    @Mock
    private AulaRepository aulaRepo;

    @Mock
    private SeccionRepository seccionRepo;

    @Mock
    private PeriodoMatriculaRepository periodoRepo;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private BloqueHorarioServiceImpl bloqueService;

    // --- Helpers ---

    private Aula crearAula(Long id, String nombre, int capacidad) {
        Aula a = new Aula();
        a.setId(id);
        a.setNombre(nombre);
        a.setCapacidad(capacidad);
        return a;
    }

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

    private Grado crearGrado(Long id, String nombre) {
        Grado g = new Grado();
        g.setId(id);
        g.setNombre(nombre);
        return g;
    }

    private com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico crearAnio(Long id, int anio) {
        com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico a =
                new com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico();
        a.setId(id);
        a.setAnio(anio);
        a.setFechaInicio(java.time.LocalDate.of(anio, 3, 1));
        a.setFechaFin(java.time.LocalDate.of(anio, 12, 15));
        return a;
    }

    private Seccion crearSeccion(Long id, String nombre, Long gradoId, String gradoNombre, Long anioId, int anio) {
        Seccion s = new Seccion();
        s.setId(id);
        s.setNombre(nombre);
        s.setGrado(crearGrado(gradoId, gradoNombre));
        s.setAnioAcademico(crearAnio(anioId, anio));
        return s;
    }

    private AsignacionCurso crearAsignacion(Long id, Long cursoId, String cursoNombre,
                                            Long docenteId, String docenteNombre, boolean docenteActivo,
                                            Long seccionId) {
        AsignacionCurso a = new AsignacionCurso();
        a.setId(id);
        a.setCurso(crearCurso(cursoId, cursoNombre));
        a.setDocente(crearDocente(docenteId, docenteNombre.split(" ")[0], docenteNombre.split(" ")[1], docenteActivo));
        a.setSeccion(crearSeccion(seccionId, "A", 1L, "1er Grado", 1L, 2027));
        return a;
    }

    private PeriodoMatricula crearPeriodoVigente(Long anioId) {
        PeriodoMatricula p = new PeriodoMatricula();
        p.setId(1L);
        p.setFechaInicio(LocalDate.now().minusMonths(1));
        p.setFechaFin(LocalDate.now().plusMonths(3));
        com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico a =
                new com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico();
        a.setId(anioId);
        p.setAnioAcademico(a);
        return p;
    }

    private PeriodoMatricula crearPeriodoCerrado(Long anioId) {
        PeriodoMatricula p = new PeriodoMatricula();
        p.setId(2L);
        p.setFechaInicio(LocalDate.now().minusMonths(6));
        p.setFechaFin(LocalDate.now().minusMonths(1));
        com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico a =
                new com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico();
        a.setId(anioId);
        p.setAnioAcademico(a);
        return p;
    }

    // --- Setup por defecto: PeriodoMatricula vigente para anioAcademico ID 1 ---
    @BeforeEach
    void setUp() {
        PeriodoMatricula periodoVigente = crearPeriodoVigente(1L);
        lenient().when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(periodoVigente));
    }

    // --- Tests ---

    @Test
    void crearBloque_exitoso() {
        // Arrange
        Aula aula = crearAula(1L, "Aula 101", 30);
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);

        when(asignacionRepo.findById(1L)).thenReturn(Optional.of(asignacion));
        when(aulaRepo.findById(1L)).thenReturn(Optional.of(aula));
        when(bloqueRepo.findConflictoDocente(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(bloqueRepo.findConflictoSeccion(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(bloqueRepo.findConflictoAula(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(bloqueRepo.save(any(BloqueHorario.class))).thenAnswer(inv -> {
            BloqueHorario b = inv.getArgument(0);
            b.setId(10L);
            return b;
        });

        CrearBloqueRequest request = new CrearBloqueRequest();
        request.setAsignacionCursoId(1L);
        request.setAulaId(1L);
        request.setDiaSemana(DiaSemana.LUNES);
        request.setHoraInicio(LocalTime.of(8, 0));
        request.setHoraFin(LocalTime.of(10, 0));

        // Act
        BloqueHorarioResponse resultado = bloqueService.crear(request);

        // Assert
        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals(DiaSemana.LUNES, resultado.getDiaSemana());
        assertEquals(LocalTime.of(8, 0), resultado.getHoraInicio());
        assertEquals(LocalTime.of(10, 0), resultado.getHoraFin());
        assertEquals(1L, resultado.getAulaId());
        assertEquals("Aula 101", resultado.getAulaNombre());
        assertEquals(1L, resultado.getAsignacionCursoId());
        assertEquals(1L, resultado.getCursoId());
        assertEquals("Matematicas", resultado.getCursoNombre());
        assertEquals(2L, resultado.getDocenteId());

        verify(bloqueRepo).save(any(BloqueHorario.class));
    }

    @Test
    void crearBloque_programacionCerrada_sinPeriodo() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(asignacionRepo.findById(1L)).thenReturn(Optional.of(asignacion));
        when(aulaRepo.findById(1L)).thenReturn(Optional.of(aula));
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.empty());

        CrearBloqueRequest request = new CrearBloqueRequest();
        request.setAsignacionCursoId(1L);
        request.setAulaId(1L);
        request.setDiaSemana(DiaSemana.LUNES);
        request.setHoraInicio(LocalTime.of(8, 0));
        request.setHoraFin(LocalTime.of(10, 0));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> bloqueService.crear(request));
        assertEquals("La programación de horarios está cerrada: solo se habilita durante el periodo de matrícula.", ex.getMessage());

        verify(bloqueRepo, never()).save(any(BloqueHorario.class));
    }

    @Test
    void crearBloque_programacionCerrada_periodoVencido() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(asignacionRepo.findById(1L)).thenReturn(Optional.of(asignacion));
        when(aulaRepo.findById(1L)).thenReturn(Optional.of(aula));
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodoCerrado(1L)));

        CrearBloqueRequest request = new CrearBloqueRequest();
        request.setAsignacionCursoId(1L);
        request.setAulaId(1L);
        request.setDiaSemana(DiaSemana.LUNES);
        request.setHoraInicio(LocalTime.of(8, 0));
        request.setHoraFin(LocalTime.of(10, 0));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> bloqueService.crear(request));
        assertEquals("La programación de horarios está cerrada: solo se habilita durante el periodo de matrícula.", ex.getMessage());

        verify(bloqueRepo, never()).save(any(BloqueHorario.class));
    }

    @Test
    void crearBloque_periodoNoIniciado_rechaza() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        PeriodoMatricula periodoNoIniciado = new PeriodoMatricula();
        periodoNoIniciado.setId(3L);
        periodoNoIniciado.setFechaInicio(LocalDate.now().plusDays(10));
        periodoNoIniciado.setFechaFin(LocalDate.now().plusMonths(3));

        when(asignacionRepo.findById(1L)).thenReturn(Optional.of(asignacion));
        when(aulaRepo.findById(1L)).thenReturn(Optional.of(aula));
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(periodoNoIniciado));

        CrearBloqueRequest request = new CrearBloqueRequest();
        request.setAsignacionCursoId(1L);
        request.setAulaId(1L);
        request.setDiaSemana(DiaSemana.LUNES);
        request.setHoraInicio(LocalTime.of(8, 0));
        request.setHoraFin(LocalTime.of(10, 0));

        assertThrows(ReglaNegocioException.class,
                () -> bloqueService.crear(request));

        verify(bloqueRepo, never()).save(any(BloqueHorario.class));
    }

    @Test
    void crearBloque_horaFinNoPosterior() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(asignacionRepo.findById(1L)).thenReturn(Optional.of(asignacion));
        when(aulaRepo.findById(1L)).thenReturn(Optional.of(aula));

        CrearBloqueRequest request = new CrearBloqueRequest();
        request.setAsignacionCursoId(1L);
        request.setAulaId(1L);
        request.setDiaSemana(DiaSemana.LUNES);
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(8, 0));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> bloqueService.crear(request));
        assertEquals("La hora de fin debe ser posterior a la de inicio.", ex.getMessage());

        verify(bloqueRepo, never()).save(any(BloqueHorario.class));
    }

    @Test
    void crearBloque_conflictoDocente() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(asignacionRepo.findById(1L)).thenReturn(Optional.of(asignacion));
        when(aulaRepo.findById(1L)).thenReturn(Optional.of(aula));
        when(bloqueRepo.findConflictoDocente(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new BloqueHorario()));

        CrearBloqueRequest request = new CrearBloqueRequest();
        request.setAsignacionCursoId(1L);
        request.setAulaId(1L);
        request.setDiaSemana(DiaSemana.LUNES);
        request.setHoraInicio(LocalTime.of(8, 0));
        request.setHoraFin(LocalTime.of(10, 0));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> bloqueService.crear(request));
        assertEquals("No se puede asignar al docente seleccionado en 2 horarios simultáneos.", ex.getMessage());

        verify(bloqueRepo, never()).save(any(BloqueHorario.class));
    }

    @Test
    void crearBloque_conflictoSeccion() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(asignacionRepo.findById(1L)).thenReturn(Optional.of(asignacion));
        when(aulaRepo.findById(1L)).thenReturn(Optional.of(aula));
        when(bloqueRepo.findConflictoDocente(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(bloqueRepo.findConflictoSeccion(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new BloqueHorario()));

        CrearBloqueRequest request = new CrearBloqueRequest();
        request.setAsignacionCursoId(1L);
        request.setAulaId(1L);
        request.setDiaSemana(DiaSemana.LUNES);
        request.setHoraInicio(LocalTime.of(8, 0));
        request.setHoraFin(LocalTime.of(10, 0));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> bloqueService.crear(request));
        assertEquals("No se puede asignar 2 cursos en el mismo bloque de horario.", ex.getMessage());

        verify(bloqueRepo, never()).save(any(BloqueHorario.class));
    }

    @Test
    void crearBloque_conflictoAula() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(asignacionRepo.findById(1L)).thenReturn(Optional.of(asignacion));
        when(aulaRepo.findById(1L)).thenReturn(Optional.of(aula));
        when(bloqueRepo.findConflictoDocente(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(bloqueRepo.findConflictoSeccion(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(bloqueRepo.findConflictoAula(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new BloqueHorario()));

        CrearBloqueRequest request = new CrearBloqueRequest();
        request.setAsignacionCursoId(1L);
        request.setAulaId(1L);
        request.setDiaSemana(DiaSemana.LUNES);
        request.setHoraInicio(LocalTime.of(8, 0));
        request.setHoraFin(LocalTime.of(10, 0));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> bloqueService.crear(request));
        assertEquals("El aula seleccionada ya está asignada en ese bloque de horario.", ex.getMessage());

        verify(bloqueRepo, never()).save(any(BloqueHorario.class));
    }

    @Test
    void crearBloque_asignacionNoExiste() {
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(asignacionRepo.findById(999L)).thenReturn(Optional.empty());

        CrearBloqueRequest request = new CrearBloqueRequest();
        request.setAsignacionCursoId(999L);
        request.setAulaId(1L);
        request.setDiaSemana(DiaSemana.LUNES);
        request.setHoraInicio(LocalTime.of(8, 0));
        request.setHoraFin(LocalTime.of(10, 0));

        assertThrows(RecursoNoEncontradoException.class,
                () -> bloqueService.crear(request));

        verify(bloqueRepo, never()).save(any(BloqueHorario.class));
    }

    @Test
    void crearBloque_aulaNoExiste() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);

        when(asignacionRepo.findById(1L)).thenReturn(Optional.of(asignacion));
        when(aulaRepo.findById(999L)).thenReturn(Optional.empty());

        CrearBloqueRequest request = new CrearBloqueRequest();
        request.setAsignacionCursoId(1L);
        request.setAulaId(999L);
        request.setDiaSemana(DiaSemana.LUNES);
        request.setHoraInicio(LocalTime.of(8, 0));
        request.setHoraFin(LocalTime.of(10, 0));

        assertThrows(RecursoNoEncontradoException.class,
                () -> bloqueService.crear(request));

        verify(bloqueRepo, never()).save(any(BloqueHorario.class));
    }

    @Test
    void eliminarBloque_exitoso() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);
        BloqueHorario bloque = new BloqueHorario();
        bloque.setId(5L);
        bloque.setDiaSemana(DiaSemana.LUNES);
        bloque.setHoraInicio(LocalTime.of(8, 0));
        bloque.setHoraFin(LocalTime.of(10, 0));
        bloque.setAula(aula);
        bloque.setAsignacionCurso(asignacion);

        when(bloqueRepo.findById(5L)).thenReturn(Optional.of(bloque));

        bloqueService.eliminar(5L);

        verify(bloqueRepo).delete(bloque);
    }

    @Test
    void eliminarBloque_programacionCerrada() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);
        BloqueHorario bloque = new BloqueHorario();
        bloque.setId(5L);
        bloque.setAula(aula);
        bloque.setAsignacionCurso(asignacion);

        when(bloqueRepo.findById(5L)).thenReturn(Optional.of(bloque));
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodoCerrado(1L)));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> bloqueService.eliminar(5L));
        assertEquals("La programación de horarios está cerrada: solo se habilita durante el periodo de matrícula.", ex.getMessage());

        verify(bloqueRepo, never()).delete(any(BloqueHorario.class));
    }

    @Test
    void listarBloques_porSeccion() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);
        BloqueHorario bloque = new BloqueHorario();
        bloque.setId(5L);
        bloque.setDiaSemana(DiaSemana.LUNES);
        bloque.setHoraInicio(LocalTime.of(8, 0));
        bloque.setHoraFin(LocalTime.of(10, 0));
        bloque.setAula(aula);
        bloque.setAsignacionCurso(asignacion);

        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of(bloque));

        List<BloqueHorarioResponse> resultado = bloqueService.listar(null, 1L, null, null);

        assertEquals(1, resultado.size());
        assertEquals(5L, resultado.get(0).getId());
        assertEquals(DiaSemana.LUNES, resultado.get(0).getDiaSemana());
    }
}