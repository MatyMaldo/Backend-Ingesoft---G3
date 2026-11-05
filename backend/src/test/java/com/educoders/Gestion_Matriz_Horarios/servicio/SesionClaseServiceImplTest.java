package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.SesionClaseRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GenerarSesionesRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.GenerarSesionesResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewSesionesRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewSesionesResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.SesionClaseResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.SesionOmitida;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de la generación de sesiones de clase.
 * Fechas fijas: 2027-03-01 es lunes, 2027-03-08 es lunes, 2027-03-06/07 es fin de semana.
 */
@ExtendWith(MockitoExtension.class)
class SesionClaseServiceImplTest {

    @Mock
    private SesionClaseRepository repo;

    @Mock
    private BloqueHorarioRepository bloqueRepo;

    @Mock
    private PeriodoMatriculaRepository periodoRepo;

    @Mock
    private MatriculaRepository matriculaRepo;

    @Mock
    private EstudianteRepository estudianteRepo;

    @InjectMocks
    private SesionClaseServiceImpl sesionService;

    private static final LocalDate LUNES_1 = LocalDate.of(2027, 3, 1);
    private static final LocalDate LUNES_2 = LocalDate.of(2027, 3, 8);

    // --- Helpers ---

    private BloqueHorario crearBloque(Long id, DiaSemana dia, LocalTime inicio, LocalTime fin) {
        Aula aula = new Aula();
        aula.setId(1L);
        aula.setNombre("Aula 101");
        aula.setCapacidad(30);

        Curso curso = new Curso();
        curso.setId(1L);
        curso.setNombre("Matematicas");

        Docente docente = new Docente();
        docente.setId(2L);
        docente.setNombre("Juan");
        docente.setApellidoPaterno("Perez");
        docente.setActivo(true);

        Grado grado = new Grado();
        grado.setId(1L);
        grado.setNombre("1er Grado");

        com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico anio =
                new com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico();
        anio.setId(1L);
        anio.setAnio(2027);

        Seccion seccion = new Seccion();
        seccion.setId(1L);
        seccion.setNombre("A");
        seccion.setGrado(grado);
        seccion.setAnioAcademico(anio);

        AsignacionCurso asignacion = new AsignacionCurso();
        asignacion.setId(1L);
        asignacion.setCurso(curso);
        asignacion.setDocente(docente);
        asignacion.setSeccion(seccion);

        BloqueHorario bloque = new BloqueHorario();
        bloque.setId(id);
        bloque.setDiaSemana(dia);
        bloque.setHoraInicio(inicio);
        bloque.setHoraFin(fin);
        bloque.setAula(aula);
        bloque.setAsignacionCurso(asignacion);
        return bloque;
    }

    private PeriodoMatricula crearPeriodoCerrado() {
        PeriodoMatricula p = new PeriodoMatricula();
        p.setId(1L);
        p.setFechaInicio(LocalDate.now().minusMonths(6));
        p.setFechaFin(LocalDate.now().minusDays(1));
        return p;
    }

    private PeriodoMatricula crearPeriodoAbierto() {
        PeriodoMatricula p = new PeriodoMatricula();
        p.setId(1L);
        p.setFechaInicio(LocalDate.now().minusMonths(1));
        p.setFechaFin(LocalDate.now().plusMonths(3));
        return p;
    }

    private PreviewSesionesRequest previewReq(LocalDate inicio, LocalDate fin) {
        PreviewSesionesRequest r = new PreviewSesionesRequest();
        r.setAnioAcademicoId(1L);
        r.setSeccionId(1L);
        r.setFechaInicio(inicio);
        r.setFechaFin(fin);
        return r;
    }

    private GenerarSesionesRequest generarReq(LocalDate inicio, LocalDate fin) {
        GenerarSesionesRequest r = new GenerarSesionesRequest();
        r.setAnioAcademicoId(1L);
        r.setSeccionId(1L);
        r.setFechaInicio(inicio);
        r.setFechaFin(fin);
        return r;
    }

    // --- Tests: preview ---

    @Test
    void previewSesiones_exitoso() {
        BloqueHorario bloque = crearBloque(10L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0));

        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodoCerrado()));
        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of(bloque));
        when(repo.existsByBloqueHorarioIdAndFecha(10L, LUNES_1)).thenReturn(false);

        PreviewSesionesResponse r = sesionService.previewSesiones(previewReq(LUNES_1, LUNES_1));

        assertEquals(1, r.getTotal());
        assertEquals(1, r.getSesiones().size());
        assertEquals(10L, r.getSesiones().get(0).getBloqueHorarioId());
        assertEquals(LUNES_1, r.getSesiones().get(0).getFecha());
        assertEquals(DiaSemana.LUNES, r.getSesiones().get(0).getDiaSemana());
        assertEquals("Aula 101", r.getSesiones().get(0).getAulaNombre());
        assertEquals("Matematicas", r.getSesiones().get(0).getCursoNombre());
        assertEquals("Juan Perez", r.getSesiones().get(0).getDocenteNombre());
        assertFalse(r.getSesiones().get(0).isYaExiste());
    }

    @Test
    void previewSesiones_yaExiste() {
        BloqueHorario bloque = crearBloque(10L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0));

        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodoCerrado()));
        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of(bloque));
        when(repo.existsByBloqueHorarioIdAndFecha(10L, LUNES_1)).thenReturn(true);

        PreviewSesionesResponse r = sesionService.previewSesiones(previewReq(LUNES_1, LUNES_1));

        assertEquals(1, r.getSesiones().size());
        assertTrue(r.getSesiones().get(0).isYaExiste());
    }

    @Test
    void previewSesiones_periodoAbierto() {
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodoAbierto()));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> sesionService.previewSesiones(previewReq(LUNES_1, LUNES_1)));
        assertEquals("La generación de sesiones solo se habilita después de que termine el periodo de matrícula.", ex.getMessage());
    }

    @Test
    void previewSesiones_sinPeriodo() {
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> sesionService.previewSesiones(previewReq(LUNES_1, LUNES_1)));
        assertEquals("No existe un periodo de matrícula registrado para este año académico.", ex.getMessage());
    }

    @Test
    void previewSesiones_finDeSemana() {
        BloqueHorario bloque = crearBloque(10L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0));

        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodoCerrado()));
        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of(bloque));

        LocalDate sabado = LocalDate.of(2027, 3, 6);
        LocalDate domingo = LocalDate.of(2027, 3, 7);
        PreviewSesionesResponse r = sesionService.previewSesiones(previewReq(sabado, domingo));

        assertEquals(0, r.getTotal());
    }

    // --- Tests: generar ---

    @Test
    void generarSesiones_exitoso() {
        BloqueHorario bloque = crearBloque(10L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0));

        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodoCerrado()));
        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of(bloque));
        when(repo.existsByBloqueHorarioIdAndFecha(10L, LUNES_1)).thenReturn(false);
        when(repo.save(any(SesionClase.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerarSesionesResponse r = sesionService.generarSesiones(generarReq(LUNES_1, LUNES_1));

        assertEquals(1, r.getTotalBloquesProcesados());
        assertEquals(1, r.getSesionesCreadas());
        assertEquals(0, r.getSesionesYaExistentes());
        assertEquals(0, r.getSesionesOmitidas());
        assertEquals(0, r.getErrores().size());
        verify(repo).save(any(SesionClase.class));
    }

    @Test
    void generarSesiones_idempotente() {
        BloqueHorario bloque = crearBloque(10L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0));

        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodoCerrado()));
        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of(bloque));
        when(repo.existsByBloqueHorarioIdAndFecha(10L, LUNES_1)).thenReturn(true);

        GenerarSesionesResponse r = sesionService.generarSesiones(generarReq(LUNES_1, LUNES_1));

        assertEquals(0, r.getSesionesCreadas());
        assertEquals(1, r.getSesionesYaExistentes());
        verify(repo, never()).save(any(SesionClase.class));
    }

    @Test
    void generarSesiones_conSesionesOmitidas() {
        BloqueHorario bloque = crearBloque(10L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0));

        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodoCerrado()));
        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of(bloque));
        when(repo.existsByBloqueHorarioIdAndFecha(10L, LUNES_1)).thenReturn(false);
        when(repo.save(any(SesionClase.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerarSesionesRequest req = generarReq(LUNES_1, LUNES_2);
        req.setSesionesOmitidas(List.of(new SesionOmitida(10L, LUNES_2)));

        GenerarSesionesResponse r = sesionService.generarSesiones(req);

        assertEquals(1, r.getSesionesCreadas());
        assertEquals(1, r.getSesionesOmitidas());
        verify(repo).save(any(SesionClase.class));
    }

    @Test
    void generarSesiones_rangoInvalido() {
        GenerarSesionesRequest req = generarReq(LUNES_2, LUNES_1);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> sesionService.generarSesiones(req));
        assertEquals("La fecha de fin debe ser posterior o igual a la de inicio.", ex.getMessage());
    }

    @Test
    void generarSesiones_sinBloques() {
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodoCerrado()));
        when(bloqueRepo.findByAsignacionCursoSeccionId(1L)).thenReturn(List.of());

        GenerarSesionesResponse r = sesionService.generarSesiones(generarReq(LUNES_1, LUNES_1));

        assertEquals(0, r.getTotalBloquesProcesados());
        assertEquals(0, r.getSesionesCreadas());
        assertEquals(0, r.getErrores().size());
    }

    @Test
    void generarSesiones_periodoAbierto() {
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodoAbierto()));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> sesionService.generarSesiones(generarReq(LUNES_1, LUNES_1)));
        assertEquals("La generación de sesiones solo se habilita después de que termine el periodo de matrícula.", ex.getMessage());
    }

    @Test
    void generarSesiones_sinPeriodo() {
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> sesionService.generarSesiones(generarReq(LUNES_1, LUNES_1)));
        assertEquals("No existe un periodo de matrícula registrado para este año académico.", ex.getMessage());
    }

    // --- Tests: listado por sección (HU-GEMIH-03) ---

    @Test
    void listarPorSeccion_exitoso() {
        BloqueHorario bloque = crearBloque(10L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0));
        SesionClase sesion = crearSesion(100L, bloque, LUNES_1);

        when(repo.findByBloqueHorarioAsignacionCursoSeccionIdAndFechaBetween(1L, LUNES_1, LUNES_1))
                .thenReturn(List.of(sesion));

        List<SesionClaseResponse> r = sesionService.listarPorSeccion(1L, LUNES_1, LUNES_1);

        assertEquals(1, r.size());
        assertEquals(100L, r.get(0).getId());
        assertEquals(LUNES_1, r.get(0).getFecha());
        assertEquals(LocalTime.of(8, 0), r.get(0).getHoraInicio());
        assertEquals(LocalTime.of(10, 0), r.get(0).getHoraFin());
        assertEquals("Matematicas", r.get(0).getCursoNombre());
        assertEquals("Juan Perez", r.get(0).getDocenteNombre());
        assertEquals("Aula 101", r.get(0).getAulaNombre());
        assertEquals(1L, r.get(0).getSeccionId());
        assertEquals(EstadoSesion.PROGRAMADA, r.get(0).getEstado());
    }

    @Test
    void listarPorSeccion_sinSesiones() {
        when(repo.findByBloqueHorarioAsignacionCursoSeccionIdAndFechaBetween(1L, LUNES_1, LUNES_1))
                .thenReturn(List.of());

        List<SesionClaseResponse> r = sesionService.listarPorSeccion(1L, LUNES_1, LUNES_1);

        assertTrue(r.isEmpty());
    }

    // --- Tests: listado por docente (HU-GEMIH-04) ---

    private SesionClase crearSesion(Long id, BloqueHorario bloque, LocalDate fecha) {
        SesionClase s = new SesionClase();
        s.setId(id);
        s.setFecha(fecha);
        s.setHoraInicio(LocalDateTime.of(fecha, bloque.getHoraInicio()));
        s.setHoraFin(LocalDateTime.of(fecha, bloque.getHoraFin()));
        s.setEstado(EstadoSesion.PROGRAMADA);
        s.setBloqueHorario(bloque);
        return s;
    }

    @Test
    void listarPorDocente_exitoso() {
        BloqueHorario bloque = crearBloque(10L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0));
        SesionClase sesion = crearSesion(100L, bloque, LUNES_1);

        when(repo.findByBloqueHorarioAsignacionCursoDocenteIdAndFechaBetween(2L, LUNES_1, LUNES_1))
                .thenReturn(List.of(sesion));

        List<SesionClaseResponse> r = sesionService.listarPorDocente(2L, LUNES_1, LUNES_1);

        assertEquals(1, r.size());
        assertEquals(100L, r.get(0).getId());
        assertEquals(2L, r.get(0).getDocenteId());
        assertEquals(LUNES_1, r.get(0).getFecha());
    }

    @Test
    void listarPorDocente_sinSesiones() {
        when(repo.findByBloqueHorarioAsignacionCursoDocenteIdAndFechaBetween(2L, LUNES_1, LUNES_1))
                .thenReturn(List.of());

        List<SesionClaseResponse> r = sesionService.listarPorDocente(2L, LUNES_1, LUNES_1);

        assertTrue(r.isEmpty());
    }

    // --- Tests: mi horario del docente autenticado (HU-GEMIH-05) ---

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    @Test
    void listarMiHorarioDocente_exitoso() {
        Docente docente = new Docente();
        docente.setId(2L);
        autenticarComo(docente);

        BloqueHorario bloque = crearBloque(10L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0));
        SesionClase sesion = crearSesion(100L, bloque, LUNES_1);

        when(repo.findByBloqueHorarioAsignacionCursoDocenteIdAndFechaBetween(2L, LUNES_1, LUNES_1))
                .thenReturn(List.of(sesion));

        List<SesionClaseResponse> r = sesionService.listarMiHorarioDocente(LUNES_1, LUNES_1);

        assertEquals(1, r.size());
        assertEquals(100L, r.get(0).getId());
        assertEquals(2L, r.get(0).getDocenteId());
    }

    @Test
    void listarMiHorarioDocente_rolIncorrecto() {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(5L);
        autenticarComo(estudiante);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sesionService.listarMiHorarioDocente(LUNES_1, LUNES_1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void listarMiHorarioDocente_sinAutenticacion() {
        SecurityContextHolder.clearContext();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sesionService.listarMiHorarioDocente(LUNES_1, LUNES_1));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // --- Tests: mi horario del estudiante autenticado (HU-GEMIH-06) ---

    private Matricula crearMatricula(Long id, Long seccionId, Integer anio, EstadoMatricula estado) {
        com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico anioAc =
                new com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico();
        anioAc.setId(1L);
        anioAc.setAnio(anio);

        Grado grado = new Grado();
        grado.setId(1L);
        grado.setNombre("1er Grado");

        Seccion seccion = new Seccion();
        seccion.setId(seccionId);
        seccion.setNombre("A");
        seccion.setGrado(grado);
        seccion.setAnioAcademico(anioAc);

        Matricula m = new Matricula();
        m.setId(id);
        m.setSeccion(seccion);
        m.setEstado(estado);
        return m;
    }

    @Test
    void listarMiHorarioEstudiante_exitoso() {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(5L);
        autenticarComo(estudiante);

        when(matriculaRepo.buscarMatriculasConSeccionYAnio(5L))
                .thenReturn(List.of(crearMatricula(1L, 1L, 2027, EstadoMatricula.CONFIRMADA)));

        BloqueHorario bloque = crearBloque(10L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0));
        SesionClase sesion = crearSesion(100L, bloque, LUNES_1);
        when(repo.findByBloqueHorarioAsignacionCursoSeccionIdAndFechaBetween(1L, LUNES_1, LUNES_1))
                .thenReturn(List.of(sesion));

        List<SesionClaseResponse> r = sesionService.listarMiHorarioEstudiante(LUNES_1, LUNES_1);

        assertEquals(1, r.size());
        assertEquals(100L, r.get(0).getId());
        assertEquals(1L, r.get(0).getSeccionId());
    }

    @Test
    void listarMiHorarioEstudiante_sinMatricula() {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(5L);
        autenticarComo(estudiante);

        when(matriculaRepo.buscarMatriculasConSeccionYAnio(5L)).thenReturn(List.of());

        List<SesionClaseResponse> r = sesionService.listarMiHorarioEstudiante(LUNES_1, LUNES_1);

        assertTrue(r.isEmpty());
    }

    @Test
    void listarMiHorarioEstudiante_matriculaCancelada() {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(5L);
        autenticarComo(estudiante);

        when(matriculaRepo.buscarMatriculasConSeccionYAnio(5L))
                .thenReturn(List.of(crearMatricula(1L, 1L, 2027, EstadoMatricula.CANCELADA)));

        List<SesionClaseResponse> r = sesionService.listarMiHorarioEstudiante(LUNES_1, LUNES_1);

        assertTrue(r.isEmpty());
    }

    @Test
    void listarMiHorarioEstudiante_rolIncorrecto() {
        Docente docente = new Docente();
        docente.setId(2L);
        autenticarComo(docente);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sesionService.listarMiHorarioEstudiante(LUNES_1, LUNES_1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // --- Tests: horario del estudiante a cargo del apoderado (HU-GEMIH-07) ---

    private Estudiante crearEstudiante(Long id, Long apoderadoId) {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(apoderadoId);

        Estudiante estudiante = new Estudiante();
        estudiante.setId(id);
        estudiante.setApoderado(apoderado);
        return estudiante;
    }

    @Test
    void listarPorApoderado_exitoso() {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(4L);
        autenticarComo(apoderado);

        when(estudianteRepo.findById(5L)).thenReturn(Optional.of(crearEstudiante(5L, 4L)));
        when(matriculaRepo.buscarMatriculasConSeccionYAnio(5L))
                .thenReturn(List.of(crearMatricula(1L, 1L, 2027, EstadoMatricula.CONFIRMADA)));

        BloqueHorario bloque = crearBloque(10L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0));
        SesionClase sesion = crearSesion(100L, bloque, LUNES_1);
        when(repo.findByBloqueHorarioAsignacionCursoSeccionIdAndFechaBetween(1L, LUNES_1, LUNES_1))
                .thenReturn(List.of(sesion));

        List<SesionClaseResponse> r = sesionService.listarPorApoderado(5L, LUNES_1, LUNES_1);

        assertEquals(1, r.size());
        assertEquals(100L, r.get(0).getId());
        assertEquals(1L, r.get(0).getSeccionId());
    }

    @Test
    void listarPorApoderado_noEsSuEstudiante() {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(4L);
        autenticarComo(apoderado);

        when(estudianteRepo.findById(5L)).thenReturn(Optional.of(crearEstudiante(5L, 99L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sesionService.listarPorApoderado(5L, LUNES_1, LUNES_1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void listarPorApoderado_estudianteSinMatricula() {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(4L);
        autenticarComo(apoderado);

        when(estudianteRepo.findById(5L)).thenReturn(Optional.of(crearEstudiante(5L, 4L)));
        when(matriculaRepo.buscarMatriculasConSeccionYAnio(5L)).thenReturn(List.of());

        List<SesionClaseResponse> r = sesionService.listarPorApoderado(5L, LUNES_1, LUNES_1);

        assertTrue(r.isEmpty());
    }

    @Test
    void listarPorApoderado_rolIncorrecto() {
        Docente docente = new Docente();
        docente.setId(2L);
        autenticarComo(docente);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sesionService.listarPorApoderado(5L, LUNES_1, LUNES_1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // HU-GEMIH-03: Validar fechas al consultar horario por seccion
    @Test
    void listarPorSeccion_fechasInvalidas() {
        assertThrows(ReglaNegocioException.class,
                () -> sesionService.listarPorSeccion(1L, LUNES_2, LUNES_1));
    }

    // HU-GEMIH-04: Validar fechas al consultar horario por docente
    @Test
    void listarPorDocente_fechasInvalidas() {
        assertThrows(ReglaNegocioException.class,
                () -> sesionService.listarPorDocente(2L, LUNES_2, LUNES_1));
    }

    // HU-GEMIH-06: Validar fechas del horario del estudiante
    @Test
    void listarMiHorarioEstudiante_fechasInvalidas() {
        assertThrows(ReglaNegocioException.class,
                () -> sesionService.listarMiHorarioEstudiante(LUNES_2, LUNES_1));
    }

    // HU-GEMIH-07: Validar fechas del horario por apoderado
    @Test
    void listarPorApoderado_fechasInvalidas() {
        assertThrows(ReglaNegocioException.class,
                () -> sesionService.listarPorApoderado(5L, LUNES_2, LUNES_1));
    }


    // HU-GEMIH-03: Fecha inicial obligatoria
    @Test
    void listarPorSeccion_fechaInicioNula() {
        assertThrows(ReglaNegocioException.class,
                () -> sesionService.listarPorSeccion(1L, null, LUNES_1));
    }

    // HU-GEMIH-04: Fecha final obligatoria
    @Test
    void listarPorDocente_fechaFinNula() {
        assertThrows(ReglaNegocioException.class,
                () -> sesionService.listarPorDocente(2L, LUNES_1, null));
    }

    // HU-GEMIH-05: El horario propio del docente tambien valida fechas
    @Test
    void listarMiHorarioDocente_fechasInvalidas() {
        Docente docente = new Docente();
        docente.setId(2L);
        autenticarComo(docente);

        assertThrows(ReglaNegocioException.class,
                () -> sesionService.listarMiHorarioDocente(LUNES_2, LUNES_1));
    }

    // HU-GEMIH-06: Fecha inicial obligatoria
    @Test
    void listarMiHorarioEstudiante_fechaInicioNula() {
        assertThrows(ReglaNegocioException.class,
                () -> sesionService.listarMiHorarioEstudiante(null, LUNES_1));
    }

    // HU-GEMIH-07: Fecha inicial obligatoria
    @Test
    void listarPorApoderado_fechaInicioNula() {
        assertThrows(ReglaNegocioException.class,
                () -> sesionService.listarPorApoderado(5L, null, LUNES_1));
    }

    @Test
    void listarMiHorarioEstudiante_matriculaPendiente() {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(5L);
        autenticarComo(estudiante);

        when(matriculaRepo.buscarMatriculasConSeccionYAnio(5L))
                .thenReturn(List.of(
                        crearMatricula(1L, 1L, 2027, EstadoMatricula.PENDIENTE)
                ));

        List<SesionClaseResponse> resultado =
                sesionService.listarMiHorarioEstudiante(LUNES_1, LUNES_1);

        assertTrue(resultado.isEmpty());

        verify(repo, never())
                .findByBloqueHorarioAsignacionCursoSeccionIdAndFechaBetween(
                        any(), any(), any()
                );
    }

    @Test
    void listarPorApoderado_matriculaPendiente() {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(4L);
        autenticarComo(apoderado);

        when(estudianteRepo.findById(5L))
                .thenReturn(Optional.of(crearEstudiante(5L, 4L)));

        when(matriculaRepo.buscarMatriculasConSeccionYAnio(5L))
                .thenReturn(List.of(
                        crearMatricula(1L, 1L, 2027, EstadoMatricula.PENDIENTE)
                ));

        List<SesionClaseResponse> resultado =
                sesionService.listarPorApoderado(5L, LUNES_1, LUNES_1);

        assertTrue(resultado.isEmpty());

        verify(repo, never())
                .findByBloqueHorarioAsignacionCursoSeccionIdAndFechaBetween(
                        any(), any(), any()
                );
    }


}