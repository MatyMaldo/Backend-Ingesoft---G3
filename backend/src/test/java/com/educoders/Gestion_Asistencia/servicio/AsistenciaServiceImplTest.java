package com.educoders.Gestion_Asistencia.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Asistencia.dominio.AsistenciaDocente;
import com.educoders.Gestion_Asistencia.dominio.AsistenciaEstudiante;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaDocenteRepository;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaEstudianteRepository;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaHistorialResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaSesionDetalleResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.GuardarAsistenciaEstudiantesRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Gestion_Matriz_Horarios.persistencia.SesionClaseRepository;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaServiceImplTest {

    @Mock private SesionClaseRepository sesionRepository;
    @Mock private AsistenciaEstudianteRepository asistenciaEstudianteRepository;
    @Mock private AsistenciaDocenteRepository asistenciaDocenteRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private AnioAcademicoRepository anioRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private Clock clock;

    @InjectMocks private AsistenciaServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void configurarReloj() {
        lenient().when(clock.instant()).thenReturn(Instant.now());
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static final LocalDate INICIO_ANIO = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN_ANIO = LocalDate.of(2026, 12, 15);

    private void anioDeConsulta(boolean matriculado) {
        AnioAcademico anio = new AnioAcademico();
        anio.setId(7L);
        anio.setAnio(2026);
        anio.setFechaInicio(INICIO_ANIO);
        anio.setFechaFin(FIN_ANIO);
        when(anioRepository.findFirstByFechaInicioLessThanEqualOrderByFechaInicioDescIdDesc(any(LocalDate.class)))
                .thenReturn(Optional.of(anio));
        when(matriculaRepository.existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoIn(
                eq(10L), eq(7L), any()))
                .thenReturn(matriculado);
    }

    private Docente docente() {
        Docente docente = new Docente();
        docente.setId(2L);
        docente.setNombre("Juan");
        docente.setApellidoPaterno("Perez");
        return docente;
    }

    private Estudiante estudiante() {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(10L);
        estudiante.setCodigo("EST-001");
        estudiante.setNombre("Luis");
        estudiante.setApellidoPaterno("Ramirez");
        return estudiante;
    }

    private SesionClase sesion(LocalDateTime inicio, LocalDateTime fin,
                               EstadoSesion estado, Docente docente, Estudiante estudiante) {
        Grado grado = new Grado();
        grado.setId(1L);
        grado.setNombre("1er Grado");

        Seccion seccion = new Seccion();
        seccion.setId(3L);
        seccion.setNombre("A");
        seccion.setGrado(grado);

        Curso curso = new Curso();
        curso.setId(4L);
        curso.setNombre("Matematicas");

        AsignacionCurso asignacion = new AsignacionCurso();
        asignacion.setId(5L);
        asignacion.setCurso(curso);
        asignacion.setSeccion(seccion);
        asignacion.setDocente(docente);

        BloqueHorario bloque = new BloqueHorario();
        bloque.setId(6L);
        bloque.setDiaSemana(DiaSemana.LUNES);
        bloque.setHoraInicio(inicio.toLocalTime());
        bloque.setHoraFin(fin.toLocalTime());
        bloque.setAsignacionCurso(asignacion);

        SesionClase sesion = new SesionClase();
        sesion.setId(1L);
        sesion.setFecha(inicio.toLocalDate());
        sesion.setHoraInicio(inicio);
        sesion.setHoraFin(fin);
        sesion.setEstado(estado);
        sesion.setBloqueHorario(bloque);
        return sesion;
    }

    private Matricula matricula(Estudiante estudiante) {
        Matricula matricula = new Matricula();
        matricula.setId(20L);
        matricula.setEstudiante(estudiante);
        matricula.setEstado(EstadoMatricula.CONFIRMADA);
        return matricula;
    }

    private void autenticar(Docente docente) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(docente, null, List.of()));
    }

    private void autenticar(Estudiante estudiante) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(estudiante, null, List.of()));
    }

    private void autenticar(Apoderado apoderado) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(apoderado, null, List.of()));
    }

    private void stubsDetalle(Estudiante estudiante) {
        when(matriculaRepository.buscarPorSeccionYEstadoConEstudiante(
                3L, EstadoMatricula.CONFIRMADA))
                .thenReturn(List.of(matricula(estudiante)));
        when(asistenciaEstudianteRepository.findBySesionClaseId(1L))
                .thenReturn(List.of());
        when(asistenciaDocenteRepository.findBySesionClaseId(1L))
                .thenReturn(Optional.empty());
    }

    @Test
    void detalle_devuelveRosterYPermisos() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(5);
        LocalDateTime fin = LocalDateTime.now().plusMinutes(30);
        SesionClase sesion = sesion(inicio, fin, EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        stubsDetalle(estudiante);

        AsistenciaSesionDetalleResponse r = service.obtenerDetalle(1L);

        assertEquals(1, r.estudiantes().size());
        assertEquals(10L, r.estudiantes().get(0).estudianteId());
        assertTrue(r.puedeRegistrarDocente());
        assertTrue(r.puedeRegistrarEstudiantes());
    }

    @Test
    void guardarEstudiantes_exigeRosterExacto() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        stubsDetalle(estudiante);

        GuardarAsistenciaEstudiantesRequest request =
                new GuardarAsistenciaEstudiantesRequest(List.of(
                        new GuardarAsistenciaEstudiantesRequest.AsistenciaItem(
                                10L, EstadoAsistencia.PRESENTE, "")));

        service.guardarAsistenciaEstudiantes(1L, request);

        ArgumentCaptor<List<AsistenciaEstudiante>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(asistenciaEstudianteRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(EstadoAsistencia.PRESENTE, captor.getValue().get(0).getEstado());

        ArgumentCaptor<RegistrarAuditoriaRequestDTO> auditoria =
                ArgumentCaptor.forClass(RegistrarAuditoriaRequestDTO.class);
        verify(auditoriaService).registrarAuditoria(auditoria.capture());
        assertEquals(TipoAccionAuditoria.CREAR, auditoria.getValue().getAccion());
        assertEquals(1, auditoria.getValue().getDetalles().size());
        assertEquals("Sin registro", auditoria.getValue().getDetalles().get(0).getValorAnterior());
        assertEquals("PRESENTE", auditoria.getValue().getDetalles().get(0).getValorNuevo());
    }

    @Test
    void guardarEstudiantes_siFallaLaAuditoriaPropagaElErrorParaRevertir() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(matriculaRepository.buscarPorSeccionYEstadoConEstudiante(
                3L, EstadoMatricula.CONFIRMADA))
                .thenReturn(List.of(matricula(estudiante)));
        when(asistenciaEstudianteRepository.findBySesionClaseId(1L)).thenReturn(List.of());
        doThrow(new IllegalStateException("auditoría caída"))
                .when(auditoriaService).registrarAuditoria(any());

        GuardarAsistenciaEstudiantesRequest request =
                new GuardarAsistenciaEstudiantesRequest(List.of(
                        new GuardarAsistenciaEstudiantesRequest.AsistenciaItem(
                                10L, EstadoAsistencia.PRESENTE, "")));

        assertThrows(IllegalStateException.class,
                () -> service.guardarAsistenciaEstudiantes(1L, request));
    }

    @Test
    void guardarEstudiantes_modificaLaAsistenciaYaRegistradaMientrasLaSesionEstaActiva() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.ACTIVA, docente, estudiante);

        AsistenciaEstudiante registrada = new AsistenciaEstudiante();
        registrada.setSesionClase(sesion);
        registrada.setEstudiante(estudiante);
        registrada.setEstado(EstadoAsistencia.FALTA);
        registrada.setObservaciones("");
        registrada.setFechaHoraRegistro(LocalDateTime.now().minusMinutes(2));

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(matriculaRepository.buscarPorSeccionYEstadoConEstudiante(
                3L, EstadoMatricula.CONFIRMADA))
                .thenReturn(List.of(matricula(estudiante)));
        when(asistenciaEstudianteRepository.findBySesionClaseId(1L))
                .thenReturn(List.of(registrada));

        service.guardarAsistenciaEstudiantes(1L,
                new GuardarAsistenciaEstudiantesRequest(List.of(
                        new GuardarAsistenciaEstudiantesRequest.AsistenciaItem(
                                10L, EstadoAsistencia.TARDANZA, ""))));

        ArgumentCaptor<List<AsistenciaEstudiante>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(asistenciaEstudianteRepository).saveAll(guardadas.capture());
        assertEquals(1, guardadas.getValue().size());
        assertTrue(guardadas.getValue().get(0) == registrada, "debe actualizar el mismo registro");
        assertEquals(EstadoAsistencia.TARDANZA, registrada.getEstado());

        ArgumentCaptor<RegistrarAuditoriaRequestDTO> auditoria =
                ArgumentCaptor.forClass(RegistrarAuditoriaRequestDTO.class);
        verify(auditoriaService).registrarAuditoria(auditoria.capture());
        assertEquals(TipoAccionAuditoria.ACTUALIZAR, auditoria.getValue().getAccion());
        assertEquals(1, auditoria.getValue().getDetalles().size());
        assertEquals("FALTA", auditoria.getValue().getDetalles().get(0).getValorAnterior());
        assertEquals("TARDANZA", auditoria.getValue().getDetalles().get(0).getValorNuevo());
    }

    @Test
    void guardarEstudiantes_noPermiteModificarUnaSesionFinalizada() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                EstadoSesion.FINALIZADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.guardarAsistenciaEstudiantes(1L,
                        new GuardarAsistenciaEstudiantesRequest(List.of(
                                new GuardarAsistenciaEstudiantesRequest.AsistenciaItem(
                                        10L, EstadoAsistencia.PRESENTE, "")))));
        assertEquals("La sesión ya finalizó.", error.getMessage());
        verify(asistenciaEstudianteRepository, never()).saveAll(any());
    }

    @Test
    void guardarEstudiantes_rechazaFaltantes() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(matriculaRepository.buscarPorSeccionYEstadoConEstudiante(
                3L, EstadoMatricula.CONFIRMADA))
                .thenReturn(List.of(matricula(estudiante)));

        GuardarAsistenciaEstudiantesRequest request =
                new GuardarAsistenciaEstudiantesRequest(List.of());

        assertThrows(ReglaNegocioException.class,
                () -> service.guardarAsistenciaEstudiantes(1L, request));
        verify(asistenciaEstudianteRepository, never()).saveAll(any());
    }

    @Test
    void docenteDentroDeDiezMinutosQuedaPresente() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(2),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaDocenteRepository.findBySesionClaseId(1L)).thenReturn(Optional.empty());
        when(asistenciaDocenteRepository.save(any(AsistenciaDocente.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var r = service.registrarAsistenciaDocente(1L);

        assertEquals(EstadoAsistencia.PRESENTE, r.estado());
    }

    @Test
    void docenteDespuesDeDiezMinutosQuedaTardanza() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(20),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaDocenteRepository.findBySesionClaseId(1L)).thenReturn(Optional.empty());
        when(asistenciaDocenteRepository.save(any(AsistenciaDocente.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var r = service.registrarAsistenciaDocente(1L);

        assertEquals(EstadoAsistencia.TARDANZA, r.estado());
    }

    @Test
    void docenteFueraDeVentana_rechaza() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                EstadoSesion.PROGRAMADA, docente, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaDocenteRepository.findBySesionClaseId(1L)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class,
                () -> service.registrarAsistenciaDocente(1L));
    }

    @Test
    void docenteDeOtraSesion_rechaza() {
        Docente docente = docente();
        Docente otro = new Docente();
        otro.setId(99L);
        otro.setNombre("Otro");
        otro.setApellidoPaterno("Docente");
        Estudiante estudiante = estudiante();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(30),
                EstadoSesion.PROGRAMADA, otro, estudiante);

        autenticar(docente);
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerDetalle(1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void historialEstudiante_devuelveCursosYRegistros() {
        Estudiante estudiante = estudiante();
        autenticar(estudiante);
        anioDeConsulta(true);

        Docente docente = docente();
        SesionClase sesion = sesion(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1).plusHours(2),
                EstadoSesion.FINALIZADA, docente, estudiante);
        AsistenciaEstudiante asistencia = new AsistenciaEstudiante();
        asistencia.setId(100L);
        asistencia.setSesionClase(sesion);
        asistencia.setEstudiante(estudiante);
        asistencia.setEstado(EstadoAsistencia.PRESENTE);
        asistencia.setFechaHoraRegistro(sesion.getHoraInicio());

        when(asistenciaEstudianteRepository.buscarHistorial(
                eq(10L), eq(INICIO_ANIO), eq(FIN_ANIO), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(asistencia)));
        when(asistenciaEstudianteRepository.buscarCursosHistorial(10L, INICIO_ANIO, FIN_ANIO))
                .thenReturn(List.<Object[]>of(new Object[]{4L, "Matematicas"}));
        when(asistenciaEstudianteRepository.contarPorEstado(
                10L, INICIO_ANIO, FIN_ANIO, null))
                .thenReturn(List.<Object[]>of(
                        new Object[]{EstadoAsistencia.PRESENTE, 1L}));

        AsistenciaHistorialResponse r = service.historialEstudiante(
                null, null, null, 0, 10);

        assertEquals(1, r.cursos().size());
        assertEquals(1, r.registros().size());
        assertEquals("Matematicas", r.registros().get(0).cursoNombre());
        assertEquals(0, r.pagina());
        assertEquals(10, r.tamano());
        assertEquals(1, r.totalPresentes());
        assertEquals(0, r.totalTardanzas());
        assertEquals(0, r.totalFaltas());
        assertEquals(1, r.totalRegistros());
        assertEquals(100.0, r.porcentajeAsistencia());
    }

    @Test
    void historialCalculaTotalesGlobalesYPorcentaje() {
        Estudiante estudiante = estudiante();
        autenticar(estudiante);
        anioDeConsulta(true);

        when(asistenciaEstudianteRepository.buscarHistorial(
                eq(10L), eq(INICIO_ANIO), eq(FIN_ANIO), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(), PageRequest.of(0, 10), 4));
        when(asistenciaEstudianteRepository.buscarCursosHistorial(10L, INICIO_ANIO, FIN_ANIO))
                .thenReturn(List.of());
        when(asistenciaEstudianteRepository.contarPorEstado(10L, INICIO_ANIO, FIN_ANIO, null))
                .thenReturn(List.of(
                        new Object[]{EstadoAsistencia.PRESENTE, 2L},
                        new Object[]{EstadoAsistencia.TARDANZA, 1L},
                        new Object[]{EstadoAsistencia.FALTA, 1L}));

        AsistenciaHistorialResponse r = service.historialEstudiante(
                null, null, null, 0, 10);

        assertEquals(2, r.totalPresentes());
        assertEquals(1, r.totalTardanzas());
        assertEquals(1, r.totalFaltas());
        assertEquals(0, r.totalJustificadas());
        assertEquals(4, r.totalRegistros());
        assertEquals(75.0, r.porcentajeAsistencia());
    }

    @Test
    void historialApoderado_validaEstudianteACargo() {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(4L);
        apoderado.setNombre("Carlos");
        apoderado.setApellidoPaterno("Ramirez");
        autenticar(apoderado);

        Estudiante estudiante = estudiante();
        estudiante.setApoderado(apoderado);
        anioDeConsulta(true);
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));
        when(asistenciaEstudianteRepository.buscarHistorial(
                eq(10L), eq(INICIO_ANIO), eq(FIN_ANIO), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(asistenciaEstudianteRepository.buscarCursosHistorial(10L, INICIO_ANIO, FIN_ANIO))
                .thenReturn(List.of());
        when(asistenciaEstudianteRepository.contarPorEstado(
                10L, INICIO_ANIO, FIN_ANIO, null)).thenReturn(List.of());

        AsistenciaHistorialResponse r = service.historialApoderado(
                10L, null, null, null, 0, 10);

        assertTrue(r.registros().isEmpty());
        verify(asistenciaEstudianteRepository).buscarHistorial(
                eq(10L), eq(INICIO_ANIO), eq(FIN_ANIO), isNull(), any(Pageable.class));
    }

    @Test
    void historialEstudiante_sinMatriculaConfirmadaEnElAnioDeConsultaEstaVacio() {
        autenticar(estudiante());
        anioDeConsulta(false);

        AsistenciaHistorialResponse r = service.historialEstudiante(null, null, null, 0, 10);

        assertTrue(r.registros().isEmpty());
        assertEquals(0, r.totalRegistros());
        assertEquals(2026, r.anioAcademico().anio());
        verify(asistenciaEstudianteRepository, never()).buscarHistorial(any(), any(), any(), any(), any());
    }

    @Test
    void historialEstudiante_sinAnioIniciadoEstaVacio() {
        autenticar(estudiante());
        when(anioRepository.findFirstByFechaInicioLessThanEqualOrderByFechaInicioDescIdDesc(any(LocalDate.class)))
                .thenReturn(Optional.empty());

        AsistenciaHistorialResponse r = service.historialEstudiante(null, null, null, 0, 10);

        assertTrue(r.registros().isEmpty());
        assertEquals(null, r.anioAcademico());
        verify(asistenciaEstudianteRepository, never()).buscarHistorial(any(), any(), any(), any(), any());
    }

    @Test
    void historialEstudiante_recortaElRangoPedidoAlAnioDeConsulta() {
        autenticar(estudiante());
        anioDeConsulta(true);
        when(asistenciaEstudianteRepository.buscarHistorial(
                eq(10L), eq(INICIO_ANIO), eq(LocalDate.of(2026, 6, 30)), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(asistenciaEstudianteRepository.buscarCursosHistorial(10L, INICIO_ANIO, LocalDate.of(2026, 6, 30)))
                .thenReturn(List.of());
        when(asistenciaEstudianteRepository.contarPorEstado(
                10L, INICIO_ANIO, LocalDate.of(2026, 6, 30), null)).thenReturn(List.of());

        service.historialEstudiante(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 30), null, 0, 10);

        verify(asistenciaEstudianteRepository).buscarHistorial(
                eq(10L), eq(INICIO_ANIO), eq(LocalDate.of(2026, 6, 30)), isNull(), any(Pageable.class));
    }

    @Test
    void historialEstudiante_rangoFueraDelAnioDeConsultaEstaVacio() {
        autenticar(estudiante());
        anioDeConsulta(true);

        AsistenciaHistorialResponse r = service.historialEstudiante(
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1), null, 0, 10);

        assertTrue(r.registros().isEmpty());
        verify(asistenciaEstudianteRepository, never()).buscarHistorial(any(), any(), any(), any(), any());
    }

    @Test
    void historial_justificadaNoCuentaNiComoAsistenciaNiComoFalta() {
        autenticar(estudiante());
        anioDeConsulta(true);
        when(asistenciaEstudianteRepository.buscarHistorial(
                eq(10L), eq(INICIO_ANIO), eq(FIN_ANIO), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 5));
        when(asistenciaEstudianteRepository.buscarCursosHistorial(10L, INICIO_ANIO, FIN_ANIO))
                .thenReturn(List.of());
        when(asistenciaEstudianteRepository.contarPorEstado(10L, INICIO_ANIO, FIN_ANIO, null))
                .thenReturn(List.of(
                        new Object[]{EstadoAsistencia.PRESENTE, 2L},
                        new Object[]{EstadoAsistencia.FALTA, 1L},
                        new Object[]{EstadoAsistencia.JUSTIFICADA, 2L}));

        AsistenciaHistorialResponse r = service.historialEstudiante(null, null, null, 0, 10);

        assertEquals(2, r.totalJustificadas());
        assertEquals(66.67, r.porcentajeAsistencia());
    }

    private void anioDeConsultaSinMatricula() {
        AnioAcademico anio = new AnioAcademico();
        anio.setId(7L);
        anio.setAnio(2026);
        anio.setFechaInicio(INICIO_ANIO);
        anio.setFechaFin(FIN_ANIO);
        when(anioRepository.findFirstByFechaInicioLessThanEqualOrderByFechaInicioDescIdDesc(any(LocalDate.class)))
                .thenReturn(Optional.of(anio));
    }

    @Test
    void historialDocente_informaLaHoraDeRegistroYElAnioDeConsulta() {
        Docente docente = docente();
        Estudiante estudiante = estudiante();
        autenticar(docente);
        anioDeConsultaSinMatricula();

        SesionClase sesion = sesion(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1).plusHours(2),
                EstadoSesion.FINALIZADA, docente, estudiante);
        AsistenciaDocente registro = new AsistenciaDocente();
        registro.setId(50L);
        registro.setSesionClase(sesion);
        registro.setDocente(docente);
        registro.setEstado(EstadoAsistencia.TARDANZA);
        LocalDateTime hora = sesion.getHoraInicio().plusMinutes(14);
        registro.setFechaHoraRegistro(hora);

        when(asistenciaDocenteRepository.buscarHistorial(
                eq(2L), eq(INICIO_ANIO), eq(FIN_ANIO), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(registro)));
        when(asistenciaDocenteRepository.buscarCursosHistorial(2L, INICIO_ANIO, FIN_ANIO))
                .thenReturn(List.<Object[]>of(new Object[]{4L, "Matematicas"}));
        when(asistenciaDocenteRepository.contarPorEstado(2L, INICIO_ANIO, FIN_ANIO, null))
                .thenReturn(List.<Object[]>of(new Object[]{EstadoAsistencia.TARDANZA, 1L}));

        AsistenciaHistorialResponse r = service.historialDocente(null, null, null, 0, 10);

        assertEquals(1, r.registros().size());
        assertEquals(hora, r.registros().get(0).fechaHoraRegistro());
        assertEquals(EstadoAsistencia.TARDANZA, r.registros().get(0).estado());
        assertEquals(2026, r.anioAcademico().anio());
        assertEquals(100.0, r.porcentajeAsistencia());
    }

    @Test
    void historialDocente_recortaElRangoAlAnioDeConsulta() {
        autenticar(docente());
        anioDeConsultaSinMatricula();
        when(asistenciaDocenteRepository.buscarHistorial(
                eq(2L), eq(INICIO_ANIO), eq(FIN_ANIO), eq(4L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(asistenciaDocenteRepository.buscarCursosHistorial(2L, INICIO_ANIO, FIN_ANIO))
                .thenReturn(List.of());
        when(asistenciaDocenteRepository.contarPorEstado(2L, INICIO_ANIO, FIN_ANIO, 4L))
                .thenReturn(List.of());

        service.historialDocente(LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), 4L, 0, 10);

        verify(asistenciaDocenteRepository).buscarHistorial(
                eq(2L), eq(INICIO_ANIO), eq(FIN_ANIO), eq(4L), any(Pageable.class));
    }

    @Test
    void historialDocente_sinAnioIniciadoORangoFueraDelAnioEstaVacio() {
        autenticar(docente());
        when(anioRepository.findFirstByFechaInicioLessThanEqualOrderByFechaInicioDescIdDesc(any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertTrue(service.historialDocente(null, null, null, 0, 10).registros().isEmpty());

        anioDeConsultaSinMatricula();
        assertTrue(service.historialDocente(
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1), null, 0, 10).registros().isEmpty());
        verify(asistenciaDocenteRepository, never()).buscarHistorial(any(), any(), any(), any(), any());
    }
}
