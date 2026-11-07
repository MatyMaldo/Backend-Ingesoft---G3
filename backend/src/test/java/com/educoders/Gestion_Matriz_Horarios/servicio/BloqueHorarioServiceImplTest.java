package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matriz_Horarios.config.HorarioProperties;
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.persistencia.AulaRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.BloqueMasivoItemRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.CrearBloqueRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GuardarBloquesSeccionRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GuardadoMasivoBloquesResponse;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Spy
    private HorarioProperties horarioProperties = new HorarioProperties();

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

        Seccion seccion = crearSeccion(1L, "A", 1L, "1er Grado", 1L, 2027);
        lenient().when(seccionRepo.findById(1L)).thenReturn(Optional.of(seccion));
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

    private BloqueHorario bloque(Long id, AsignacionCurso asignacion, Aula aula,
                                 DiaSemana dia, int horaInicio, int minutoInicio,
                                 int horaFin, int minutoFin) {
        BloqueHorario bloque = new BloqueHorario();
        bloque.setId(id);
        bloque.setAsignacionCurso(asignacion);
        bloque.setAula(aula);
        bloque.setDiaSemana(dia);
        bloque.setHoraInicio(LocalTime.of(horaInicio, minutoInicio));
        bloque.setHoraFin(LocalTime.of(horaFin, minutoFin));
        return bloque;
    }

    @Test
    void guardarMasivo_creaActualizaElimina() {
        AsignacionCurso asignacion1 = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        AsignacionCurso asignacion2 = crearAsignacion(2L, 2L, "Comunicacion", 3L, "Maria Gomez", true, 1L);
        Aula aula1 = crearAula(1L, "Aula 101", 30);
        Aula aula2 = crearAula(2L, "Aula 102", 30);

        BloqueHorario existente100 = bloque(100L, asignacion1, aula1, DiaSemana.LUNES, 8, 0, 10, 0);
        BloqueHorario existente101 = bloque(101L, asignacion2, aula2, DiaSemana.MARTES, 8, 0, 9, 0);

        when(bloqueRepo.findByAsignacionCursoSeccionId(1L))
                .thenReturn(List.of(existente100, existente101));
        when(asignacionRepo.findBySeccionId(1L))
                .thenReturn(List.of(asignacion1, asignacion2));
        when(aulaRepo.findAllById(any())).thenReturn(List.of(aula1, aula2));
        when(bloqueRepo.findEnOtrasSecciones(1L, 1L)).thenReturn(List.of());
        when(bloqueRepo.save(any(BloqueHorario.class))).thenAnswer(inv -> {
            BloqueHorario b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(200L);
            }
            return b;
        });

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of(
                new BloqueMasivoItemRequest(100L, 1L, 1L, DiaSemana.LUNES,
                        LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new BloqueMasivoItemRequest(null, 2L, 2L, DiaSemana.MARTES,
                        LocalTime.of(9, 0), LocalTime.of(10, 0))
        ));

        GuardadoMasivoBloquesResponse respuesta = bloqueService.guardarMasivo(1L, request);

        assertEquals(1, respuesta.creados());
        assertEquals(1, respuesta.actualizados());
        assertEquals(1, respuesta.eliminados());
        assertEquals(2, respuesta.bloques().size());
        assertEquals(LocalTime.of(9, 0), existente100.getHoraInicio());
        verify(bloqueRepo).deleteAll(List.of(existente101));
    }

    @Test
    void guardarMasivo_listaVacia_eliminaTodos() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);
        BloqueHorario existente = bloque(100L, asignacion, aula, DiaSemana.LUNES, 8, 0, 10, 0);

        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of(existente));
        when(asignacionRepo.findBySeccionId(1L)).thenReturn(List.of(asignacion));
        when(aulaRepo.findAllById(any())).thenReturn(List.of(aula));
        when(bloqueRepo.findEnOtrasSecciones(1L, 1L)).thenReturn(List.of());

        GuardadoMasivoBloquesResponse respuesta = bloqueService.guardarMasivo(
                1L, new GuardarBloquesSeccionRequest(1L, List.of()));

        assertEquals(0, respuesta.creados());
        assertEquals(0, respuesta.actualizados());
        assertEquals(1, respuesta.eliminados());
        verify(bloqueRepo).deleteAll(List.of(existente));
    }

    @Test
    void guardarMasivo_periodoCerrado_rechaza() {
        when(periodoRepo.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodoCerrado(1L)));

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of());

        assertThrows(ReglaNegocioException.class,
                () -> bloqueService.guardarMasivo(1L, request));
    }

    @Test
    void guardarMasivo_seccionNoPerteneceAnio_rechaza() {
        Seccion seccion = crearSeccion(1L, "A", 1L, "1er Grado", 2L, 2026);
        when(seccionRepo.findById(1L)).thenReturn(Optional.of(seccion));

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of());

        assertThrows(ReglaNegocioException.class,
                () -> bloqueService.guardarMasivo(1L, request));
    }

    @Test
    void guardarMasivo_horaInvalida_rechaza() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of());
        when(asignacionRepo.findBySeccionId(1L)).thenReturn(List.of(asignacion));
        when(aulaRepo.findAllById(any())).thenReturn(List.of(aula));

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of(
                new BloqueMasivoItemRequest(null, 1L, 1L, DiaSemana.LUNES,
                        LocalTime.of(10, 0), LocalTime.of(9, 0))
        ));

        assertThrows(ReglaNegocioException.class,
                () -> bloqueService.guardarMasivo(1L, request));
    }

    @Test
    void guardarMasivo_respetaConfiguracionDeJornada() {
        horarioProperties.setJornadaInicio("09:00");

        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of());
        when(asignacionRepo.findBySeccionId(1L)).thenReturn(List.of(asignacion));
        when(aulaRepo.findAllById(any())).thenReturn(List.of(aula));

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of(
                new BloqueMasivoItemRequest(null, 1L, 1L, DiaSemana.LUNES,
                        LocalTime.of(8, 0), LocalTime.of(9, 0))
        ));

        assertThrows(ReglaNegocioException.class,
                () -> bloqueService.guardarMasivo(1L, request));
    }

    @Test
    void guardarMasivo_cruceReceso_rechaza() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of());
        when(asignacionRepo.findBySeccionId(1L)).thenReturn(List.of(asignacion));
        when(aulaRepo.findAllById(any())).thenReturn(List.of(aula));

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of(
                new BloqueMasivoItemRequest(null, 1L, 1L, DiaSemana.LUNES,
                        LocalTime.of(10, 30), LocalTime.of(11, 15))
        ));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> bloqueService.guardarMasivo(1L, request));
        assertTrue(ex.getMessage().contains("receso"));
    }

    @Test
    void guardarMasivo_fueraDeJornada_rechaza() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of());
        when(asignacionRepo.findBySeccionId(1L)).thenReturn(List.of(asignacion));
        when(aulaRepo.findAllById(any())).thenReturn(List.of(aula));

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of(
                new BloqueMasivoItemRequest(null, 1L, 1L, DiaSemana.LUNES,
                        LocalTime.of(7, 0), LocalTime.of(8, 0))
        ));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> bloqueService.guardarMasivo(1L, request));
        assertTrue(ex.getMessage().contains("jornada"));
    }

    @Test
    void guardarMasivo_choqueDocenteEnPayload_rechaza() {
        AsignacionCurso asignacion1 = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        AsignacionCurso asignacion2 = crearAsignacion(2L, 2L, "Comunicacion", 2L, "Juan Perez", true, 1L);
        Aula aula1 = crearAula(1L, "Aula 101", 30);
        Aula aula2 = crearAula(2L, "Aula 102", 30);

        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of());
        when(asignacionRepo.findBySeccionId(1L)).thenReturn(List.of(asignacion1, asignacion2));
        when(aulaRepo.findAllById(any())).thenReturn(List.of(aula1, aula2));

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of(
                new BloqueMasivoItemRequest(null, 1L, 1L, DiaSemana.LUNES,
                        LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new BloqueMasivoItemRequest(null, 2L, 2L, DiaSemana.LUNES,
                        LocalTime.of(9, 0), LocalTime.of(10, 0))
        ));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> bloqueService.guardarMasivo(1L, request));
        assertTrue(ex.getMessage().contains("docente"));
    }

    @Test
    void guardarMasivo_choqueAulaEnPayload_rechaza() {
        AsignacionCurso asignacion1 = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        AsignacionCurso asignacion2 = crearAsignacion(2L, 2L, "Comunicacion", 3L, "Maria Gomez", true, 1L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of());
        when(asignacionRepo.findBySeccionId(1L)).thenReturn(List.of(asignacion1, asignacion2));
        when(aulaRepo.findAllById(any())).thenReturn(List.of(aula));

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of(
                new BloqueMasivoItemRequest(null, 1L, 1L, DiaSemana.LUNES,
                        LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new BloqueMasivoItemRequest(null, 2L, 1L, DiaSemana.LUNES,
                        LocalTime.of(9, 0), LocalTime.of(10, 0))
        ));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> bloqueService.guardarMasivo(1L, request));
        assertTrue(ex.getMessage().contains("aula"));
    }

    @Test
    void guardarMasivo_choqueSeccionEnPayload_rechaza() {
        AsignacionCurso asignacion1 = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        AsignacionCurso asignacion2 = crearAsignacion(2L, 2L, "Comunicacion", 3L, "Maria Gomez", true, 1L);
        Aula aula1 = crearAula(1L, "Aula 101", 30);
        Aula aula2 = crearAula(2L, "Aula 102", 30);

        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of());
        when(asignacionRepo.findBySeccionId(1L)).thenReturn(List.of(asignacion1, asignacion2));
        when(aulaRepo.findAllById(any())).thenReturn(List.of(aula1, aula2));

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of(
                new BloqueMasivoItemRequest(null, 1L, 1L, DiaSemana.LUNES,
                        LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new BloqueMasivoItemRequest(null, 2L, 2L, DiaSemana.LUNES,
                        LocalTime.of(9, 0), LocalTime.of(10, 0))
        ));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> bloqueService.guardarMasivo(1L, request));
        assertTrue(ex.getMessage().contains("sección"));
    }

    @Test
    void guardarMasivo_choqueDocenteOtraSeccion_rechaza() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        AsignacionCurso asignacionOtra = crearAsignacion(9L, 9L, "Comunicacion", 2L, "Juan Perez", true, 2L);
        Aula aula = crearAula(1L, "Aula 101", 30);

        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of());
        when(asignacionRepo.findBySeccionId(1L)).thenReturn(List.of(asignacion));
        when(aulaRepo.findAllById(any())).thenReturn(List.of(aula));
        when(bloqueRepo.findEnOtrasSecciones(1L, 1L)).thenReturn(List.of(
                bloque(500L, asignacionOtra, aula, DiaSemana.LUNES, 8, 0, 10, 0)
        ));

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of(
                new BloqueMasivoItemRequest(null, 1L, 1L, DiaSemana.LUNES,
                        LocalTime.of(9, 0), LocalTime.of(10, 0))
        ));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> bloqueService.guardarMasivo(1L, request));
        assertTrue(ex.getMessage().contains("otra sección"));
    }

    @Test
    void guardarMasivo_aulaNoExiste_rechaza() {
        AsignacionCurso asignacion = crearAsignacion(1L, 1L, "Matematicas", 2L, "Juan Perez", true, 1L);
        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of());
        when(asignacionRepo.findBySeccionId(1L)).thenReturn(List.of(asignacion));
        when(aulaRepo.findAllById(any())).thenReturn(List.of());

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of(
                new BloqueMasivoItemRequest(null, 1L, 99L, DiaSemana.LUNES,
                        LocalTime.of(8, 0), LocalTime.of(10, 0))
        ));

        assertThrows(RecursoNoEncontradoException.class,
                () -> bloqueService.guardarMasivo(1L, request));
    }

    @Test
    void guardarMasivo_bloqueIdDeOtraSeccion_rechaza() {
        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of());
        when(bloqueRepo.findById(999L)).thenReturn(Optional.of(
                bloque(999L, crearAsignacion(9L, 9L, "Otra", 3L, "Maria Gomez", true, 2L),
                        crearAula(9L, "Aula 999", 30), DiaSemana.LUNES, 8, 0, 10, 0)));

        GuardarBloquesSeccionRequest request = new GuardarBloquesSeccionRequest(1L, List.of(
                new BloqueMasivoItemRequest(999L, 9L, 9L, DiaSemana.LUNES,
                        LocalTime.of(8, 0), LocalTime.of(10, 0))
        ));

        assertThrows(ReglaNegocioException.class,
                () -> bloqueService.guardarMasivo(1L, request));
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