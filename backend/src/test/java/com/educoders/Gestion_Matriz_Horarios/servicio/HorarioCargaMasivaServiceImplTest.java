package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.CursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.GradoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matriz_Horarios.config.HorarioProperties;
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.persistencia.AulaRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ConfirmarCargaMasivaRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ConfirmarCargaMasivaResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ExportarBorradorRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewCargaMasivaResponse;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.operaciones.ProgresoLote;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorarioCargaMasivaServiceImplTest {

    @Mock private PeriodoMatriculaRepository periodoRepo;
    @Mock private AnioAcademicoRepository anioRepo;
    @Mock private GradoRepository gradoRepo;
    @Mock private SeccionRepository seccionRepo;
    @Mock private CursoRepository cursoRepo;
    @Mock private DocenteRepository docenteRepo;
    @Mock private AulaRepository aulaRepo;
    @Mock private AsignacionCursoRepository asignacionRepo;
    @Mock private BloqueHorarioRepository bloqueRepo;
    @Spy private HorarioProperties horarioProperties = new HorarioProperties();
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private HorarioCargaMasivaServiceImpl service;

    private AnioAcademico anio() {
        AnioAcademico a = new AnioAcademico();
        a.setId(1L);
        a.setAnio(2027);
        a.setFechaInicio(LocalDate.now().minusMonths(1));
        a.setFechaFin(LocalDate.now().plusMonths(6));
        return a;
    }

    private PeriodoMatricula periodo() {
        PeriodoMatricula p = new PeriodoMatricula();
        p.setFechaInicio(LocalDate.now().minusDays(10));
        p.setFechaFin(LocalDate.now().plusDays(20));
        return p;
    }

    private Grado grado() {
        Grado g = new Grado();
        g.setId(1L);
        g.setNombre("1er Grado");
        return g;
    }

    private Seccion seccion(AnioAcademico anio) {
        Seccion s = new Seccion();
        s.setId(1L);
        s.setNombre("A");
        s.setGrado(grado());
        s.setAnioAcademico(anio);
        return s;
    }

    private Docente docente() {
        Docente d = new Docente();
        d.setId(2L);
        d.setNombre("Juan");
        d.setApellidoPaterno("Perez");
        d.setDni("1234567");
        d.setActivo(true);
        return d;
    }

    private AsignacionCurso asignacion(AnioAcademico anio) {
        Curso c = new Curso();
        c.setId(1L);
        c.setNombre("Matemática");
        AsignacionCurso a = new AsignacionCurso();
        a.setId(10L);
        a.setCurso(c);
        a.setDocente(docente());
        a.setSeccion(seccion(anio));
        return a;
    }

    private Aula aula() {
        Aula a = new Aula();
        a.setId(1L);
        a.setNombre("Aula 101");
        a.setCapacidad(30);
        return a;
    }

    private void stubsPreview(AnioAcademico anio, AsignacionCurso asignacion) {
        lenient().when(anioRepo.findById(1L)).thenReturn(Optional.of(anio));
        lenient().when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(periodo()));
        lenient().when(gradoRepo.findByNombre("1er Grado")).thenReturn(Optional.of(grado()));
        lenient().when(seccionRepo.findByGradoIdAndAnioAcademicoIdAndNombre(1L, 1L, "A"))
                .thenReturn(Optional.of(seccion(anio)));
        lenient().when(cursoRepo.findByNombre("Matemática")).thenReturn(Optional.of(asignacion.getCurso()));
        lenient().when(docenteRepo.findByDni("1234567")).thenReturn(Optional.of(docente()));
        lenient().when(aulaRepo.findByNombre("Aula 101")).thenReturn(Optional.of(aula()));
        lenient().when(aulaRepo.findById(1L)).thenReturn(Optional.of(aula()));
        lenient().when(asignacionRepo.findByCursoIdAndSeccionId(1L, 1L))
                .thenReturn(Optional.of(asignacion));
        lenient().when(asignacionRepo.findById(10L)).thenReturn(Optional.of(asignacion));
        lenient().when(bloqueRepo.findConflictoDocente(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of());
        lenient().when(bloqueRepo.findConflictoSeccion(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of());
        lenient().when(bloqueRepo.findConflictoAula(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of());
    }

    @Test
    void previewConCsvValido() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        String csv = "Grado;Sección;Curso;DNI Docente;Aula;Día;Hora inicio;Hora fin\n"
                + "1er Grado;A;Matemática;1234567;Aula 101;LUNES;08:00;09:00\n";
        MockMultipartFile archivo = new MockMultipartFile(
                "files", "1A.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        PreviewCargaMasivaResponse respuesta = service.preview(List.of(archivo), 1L);

        assertEquals(1, respuesta.resumen().filas());
        assertEquals(1, respuesta.resumen().validas());
        assertEquals("VALIDO", respuesta.bloques().get(0).estado());
    }

    @Test
    void previewAcumulaConflictosConNombres() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        BloqueHorario existente = new BloqueHorario();
        existente.setId(500L);
        existente.setDiaSemana(DiaSemana.LUNES);
        existente.setHoraInicio(LocalTime.of(8, 0));
        existente.setHoraFin(LocalTime.of(9, 0));
        existente.setAula(aula());
        existente.setAsignacionCurso(asignacion);

        when(bloqueRepo.findConflictoDocente(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of(existente));
        when(bloqueRepo.findConflictoSeccion(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of(existente));
        when(bloqueRepo.findConflictoAula(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of(existente));

        String csv = "Grado;Sección;Curso;DNI Docente;Aula;Día;Hora inicio;Hora fin\n"
                + "1er Grado;A;Matemática;1234567;Aula 101;LUNES;08:00;09:00\n";
        MockMultipartFile archivo = new MockMultipartFile(
                "files", "1A.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        PreviewCargaMasivaResponse respuesta = service.preview(List.of(archivo), 1L);

        assertEquals("CONFLICTO", respuesta.bloques().get(0).estado());
        assertEquals(3, respuesta.bloques().get(0).errores().size());
        assertTrue(respuesta.bloques().get(0).errores().stream()
                .anyMatch(m -> m.contains("docente")));
        assertTrue(respuesta.bloques().get(0).errores().stream()
                .anyMatch(m -> m.contains("sección")));
        assertTrue(respuesta.bloques().get(0).errores().stream()
                .anyMatch(m -> m.contains("aula")));
    }

    @Test
    void previewDuplicadoReportaSoloUnMensaje() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        String csv = "Grado;Sección;Curso;DNI Docente;Aula;Día;Hora inicio;Hora fin\n"
                + "1er Grado;A;Matemática;1234567;Aula 101;LUNES;08:00;09:00\n"
                + "1er Grado;A;Matemática;1234567;Aula 101;LUNES;08:00;09:00\n";
        MockMultipartFile archivo = new MockMultipartFile(
                "files", "1A.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        PreviewCargaMasivaResponse respuesta = service.preview(List.of(archivo), 1L);

        assertEquals(1, respuesta.resumen().validas());
        assertEquals(1, respuesta.resumen().duplicadas());
        assertEquals("DUPLICADO", respuesta.bloques().get(1).estado());
        assertEquals(1, respuesta.bloques().get(1).errores().size());
        assertTrue(respuesta.bloques().get(1).errores().get(0).contains("duplica"));
    }

    @Test
    void confirmarCreaBloquesSiTodoEsValido() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);
        when(bloqueRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfirmarCargaMasivaResponse respuesta = service.confirmar(
                new ConfirmarCargaMasivaRequest(1L, List.of(
                        new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                                10L, 1L, DiaSemana.LUNES,
                                LocalTime.of(8, 0), LocalTime.of(9, 0), "1A.csv#2"))));

        assertEquals(1, respuesta.bloquesCreados());
        assertTrue(respuesta.errores().isEmpty());
        verify(bloqueRepo).saveAll(any());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void confirmarAsyncUsaUsuarioExplicitoYProgreso() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);
        when(bloqueRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ProgresoGrabado progreso = new ProgresoGrabado();
        ConfirmarCargaMasivaResponse respuesta = service.confirmar(
                new ConfirmarCargaMasivaRequest(1L, List.of(
                        new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                                10L, 1L, DiaSemana.LUNES,
                                LocalTime.of(8, 0), LocalTime.of(9, 0), "1A.csv#2"))),
                55L,
                progreso);

        assertEquals(1, respuesta.bloquesCreados());
        assertEquals(1, progreso.totalIniciado);
        assertEquals(1, progreso.ultimoExitoso);

        ArgumentCaptor<RegistrarAuditoriaRequestDTO> captor =
                ArgumentCaptor.forClass(RegistrarAuditoriaRequestDTO.class);
        verify(auditoriaService).registrarAuditoria(captor.capture());
        assertEquals(55L, captor.getValue().getUsuarioId());
    }

    @Test
    void confirmarNoCreaNadaSiHayConflicto() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        BloqueHorario existente = new BloqueHorario();
        existente.setId(500L);
        existente.setDiaSemana(DiaSemana.LUNES);
        existente.setHoraInicio(LocalTime.of(8, 0));
        existente.setHoraFin(LocalTime.of(9, 0));
        existente.setAula(aula());
        existente.setAsignacionCurso(asignacion);
        when(bloqueRepo.findConflictoAula(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of(existente));

        ConfirmarCargaMasivaResponse respuesta = service.confirmar(
                new ConfirmarCargaMasivaRequest(1L, List.of(
                        new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                                10L, 1L, DiaSemana.LUNES,
                                LocalTime.of(8, 0), LocalTime.of(9, 0), "1A.csv#2"))));

        assertEquals(0, respuesta.bloquesCreados());
        assertFalse(respuesta.errores().isEmpty());
        assertTrue(respuesta.errores().stream()
                .anyMatch(e -> e.mensaje().contains("aula")));
        verify(bloqueRepo, never()).saveAll(any());
    }

    @Test
    void confirmarAcumulaDocenteSeccionYAulaEnLaMismaFila() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        BloqueHorario existente = new BloqueHorario();
        existente.setId(500L);
        existente.setDiaSemana(DiaSemana.LUNES);
        existente.setHoraInicio(LocalTime.of(8, 0));
        existente.setHoraFin(LocalTime.of(9, 0));
        existente.setAula(aula());
        existente.setAsignacionCurso(asignacion);

        when(bloqueRepo.findConflictoDocente(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of(existente));
        when(bloqueRepo.findConflictoSeccion(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of(existente));
        when(bloqueRepo.findConflictoAula(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of(existente));

        ConfirmarCargaMasivaResponse respuesta = service.confirmar(
                new ConfirmarCargaMasivaRequest(1L, List.of(
                        new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                                10L, 1L, DiaSemana.LUNES,
                                LocalTime.of(8, 0), LocalTime.of(9, 0), "1A.csv#2"))));

        assertEquals(0, respuesta.bloquesCreados());
        assertEquals(3, respuesta.errores().size());
        assertTrue(respuesta.errores().stream().anyMatch(e -> e.mensaje().contains("docente")));
        assertTrue(respuesta.errores().stream().anyMatch(e -> e.mensaje().contains("sección")));
        assertTrue(respuesta.errores().stream().anyMatch(e -> e.mensaje().contains("aula")));
        verify(bloqueRepo, never()).saveAll(any());
    }

    @Test
    void confirmarDuplicadoEnLaSolicitudReportaUnSoloError() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        ConfirmarCargaMasivaRequest request = new ConfirmarCargaMasivaRequest(1L, List.of(
                new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                        10L, 1L, DiaSemana.LUNES,
                        LocalTime.of(8, 0), LocalTime.of(9, 0), "fila-1"),
                new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                        10L, 1L, DiaSemana.LUNES,
                        LocalTime.of(8, 0), LocalTime.of(9, 0), "fila-2")
        ));

        ConfirmarCargaMasivaResponse respuesta = service.confirmar(request);

        assertEquals(0, respuesta.bloquesCreados());
        assertEquals(1, respuesta.errores().size());
        assertTrue(respuesta.errores().get(0).mensaje().contains("duplica"));
        verify(bloqueRepo, never()).saveAll(any());
    }

    @Test
    void exportarBorradorGeneraCsvSinGuardar() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        byte[] bytes = service.exportarBorrador(new ExportarBorradorRequest(
                1L,
                List.of(new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                        10L, 1L, DiaSemana.LUNES,
                        LocalTime.of(8, 0), LocalTime.of(9, 0), null)),
                "csv"));

        String texto = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(texto.contains("Matemática"));
        assertTrue(texto.contains("Aula 101"));
        assertTrue(texto.contains("08:00"));
        verify(bloqueRepo, never()).saveAll(any());
    }

    @Test
    void plantillaCsvIncluyeEncabezadosCompletos() {
        String texto = new String(service.generarPlantilla("csv"), StandardCharsets.UTF_8);
        assertTrue(texto.contains("ID Sección"));
        assertTrue(texto.contains("DNI Docente"));
    }

    @Test
    void plantillaXlsxGeneraArchivoExcel() {
        byte[] bytes = service.generarPlantilla("xlsx");
        assertTrue(bytes.length > 2);
        assertEquals((byte) 'P', bytes[0]);
        assertEquals((byte) 'K', bytes[1]);
    }

    private static class ProgresoGrabado implements ProgresoLote {
        private int totalIniciado = -1;
        private int ultimoExitoso = -1;

        @Override
        public void iniciar(int total) {
            this.totalIniciado = total;
        }

        @Override
        public void actualizar(int procesados, int exitosos, int omitidos, int errores) {
            this.ultimoExitoso = exitosos;
        }

        @Override
        public void registrarError(String mensaje) {
        }

        @Override
        public void fase(String mensaje) {
        }
    }
}
