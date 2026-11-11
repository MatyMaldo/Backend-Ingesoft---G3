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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        lenient().when(asignacionRepo.findByAnioAcademicoIdConDetalle(1L))
                .thenReturn(List.of(asignacion));
        lenient().when(bloqueRepo.findDelAnioConDetalle(1L)).thenReturn(List.of());
    }

    private BloqueHorario bloqueExistente(AsignacionCurso asignacion, Aula aula,
                                          LocalTime inicio, LocalTime fin) {
        BloqueHorario existente = new BloqueHorario();
        existente.setId(500L);
        existente.setDiaSemana(DiaSemana.LUNES);
        existente.setHoraInicio(inicio);
        existente.setHoraFin(fin);
        existente.setAula(aula);
        existente.setAsignacionCurso(asignacion);
        return existente;
    }

    /** Asignación de otra sección y otro docente: solo puede chocar por aula. */
    private AsignacionCurso asignacionAjena(AnioAcademico anio) {
        AsignacionCurso ajena = asignacion(anio);
        ajena.setId(11L);
        Docente otro = docente();
        otro.setId(3L);
        ajena.setDocente(otro);
        Seccion otraSeccion = seccion(anio);
        otraSeccion.setId(2L);
        otraSeccion.setNombre("B");
        ajena.setSeccion(otraSeccion);
        return ajena;
    }

    private MockMultipartFile csv(String... filas) {
        String contenido = "Grado;Sección;Curso;DNI Docente;Aula;Día;Hora inicio;Hora fin\n"
                + String.join("\n", filas) + "\n";
        return new MockMultipartFile(
                "files", "1A.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
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

        BloqueHorario existente = bloqueExistente(
                asignacion, aula(), LocalTime.of(8, 0), LocalTime.of(9, 0));
        when(bloqueRepo.findDelAnioConDetalle(1L)).thenReturn(List.of(existente));

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

        ConfirmarCargaMasivaResponse respuesta = service.confirmar(
                new ConfirmarCargaMasivaRequest(1L, List.of(
                        new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                                10L, 1L, DiaSemana.LUNES,
                                LocalTime.of(8, 0), LocalTime.of(9, 0), "1A.csv#2"))));

        assertEquals(1, respuesta.bloquesCreados());
        assertTrue(respuesta.errores().isEmpty());
        verify(bloqueRepo).insertarEnLotes(any());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void confirmarAsyncUsaUsuarioExplicitoYProgreso() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

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

        BloqueHorario existente = bloqueExistente(
                asignacionAjena(anio), aula(), LocalTime.of(8, 0), LocalTime.of(9, 0));
        when(bloqueRepo.findDelAnioConDetalle(1L)).thenReturn(List.of(existente));

        ConfirmarCargaMasivaResponse respuesta = service.confirmar(
                new ConfirmarCargaMasivaRequest(1L, List.of(
                        new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                                10L, 1L, DiaSemana.LUNES,
                                LocalTime.of(8, 0), LocalTime.of(9, 0), "1A.csv#2"))));

        assertEquals(0, respuesta.bloquesCreados());
        assertEquals(1, respuesta.errores().size());
        assertTrue(respuesta.errores().get(0).mensaje().contains("aula"));
        verify(bloqueRepo, never()).insertarEnLotes(any());
    }

    @Test
    void confirmarAcumulaDocenteSeccionYAulaEnLaMismaFila() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        BloqueHorario existente = bloqueExistente(
                asignacion, aula(), LocalTime.of(8, 0), LocalTime.of(9, 0));
        when(bloqueRepo.findDelAnioConDetalle(1L)).thenReturn(List.of(existente));

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
        verify(bloqueRepo, never()).insertarEnLotes(any());
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
        verify(bloqueRepo, never()).insertarEnLotes(any());
    }

    @Test
    void previewCargaCatalogosUnaVezYNoConsultaConflictosPorFila() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        PreviewCargaMasivaResponse respuesta = service.preview(List.of(csv(
                "1er Grado;A;Matemática;1234567;Aula 101;LUNES;08:00;09:00",
                "1er Grado;A;Matemática;1234567;Aula 101;MARTES;08:00;09:00",
                "1er Grado;A;Matemática;1234567;Aula 101;MIERCOLES;08:00;09:00")), 1L);

        assertEquals(3, respuesta.resumen().validas());
        verify(asignacionRepo, times(1)).findByAnioAcademicoIdConDetalle(1L);
        verify(bloqueRepo, times(1)).findDelAnioConDetalle(1L);
        verify(bloqueRepo, never()).findConflictoDocente(any(), any(), any(), anyLong(), any(), anyLong());
        verify(bloqueRepo, never()).findConflictoSeccion(any(), any(), any(), anyLong(), any(), anyLong());
        verify(bloqueRepo, never()).findConflictoAula(any(), any(), any(), anyLong(), any(), anyLong());
        verify(asignacionRepo, never()).findByCursoIdAndSeccionId(any(), any());
        verify(gradoRepo, times(1)).findByNombre("1er Grado");
        verify(cursoRepo, times(1)).findByNombre("Matemática");
        verify(docenteRepo, times(1)).findByDni("1234567");
        verify(aulaRepo, times(1)).findByNombre("Aula 101");
    }

    @Test
    void previewMemoizaTambienLosValoresQueNoExisten() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        PreviewCargaMasivaResponse respuesta = service.preview(List.of(csv(
                "1er Grado;A;Inexistente;1234567;Aula 101;LUNES;08:00;09:00",
                "1er Grado;A;Inexistente;1234567;Aula 101;MARTES;08:00;09:00",
                "1er Grado;A;Inexistente;1234567;Aula 101;MIERCOLES;08:00;09:00")), 1L);

        assertEquals(3, respuesta.resumen().conError());
        assertTrue(respuesta.bloques().get(0).errores().get(0).contains("No existe el curso 'Inexistente'"));
        verify(cursoRepo, times(1)).findByNombre("Inexistente");
    }

    @Test
    void previewNoTrataBloquesContiguosComoConflicto() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);
        when(bloqueRepo.findDelAnioConDetalle(1L)).thenReturn(List.of(bloqueExistente(
                asignacion, aula(), LocalTime.of(8, 0), LocalTime.of(9, 0))));

        PreviewCargaMasivaResponse respuesta = service.preview(List.of(csv(
                "1er Grado;A;Matemática;1234567;Aula 101;LUNES;09:00;10:00")), 1L);

        assertEquals("VALIDO", respuesta.bloques().get(0).estado());
    }

    @Test
    void previewDetectaSolapeEntreFilasDelMismoArchivo() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        PreviewCargaMasivaResponse respuesta = service.preview(List.of(csv(
                "1er Grado;A;Matemática;1234567;Aula 101;LUNES;08:00;09:00",
                "1er Grado;A;Matemática;1234567;Aula 101;LUNES;08:30;09:30")), 1L);

        assertEquals("VALIDO", respuesta.bloques().get(0).estado());
        assertEquals("CONFLICTO", respuesta.bloques().get(1).estado());
        assertEquals(3, respuesta.bloques().get(1).errores().size());
        assertTrue(respuesta.bloques().get(1).errores().stream()
                .allMatch(m -> m.contains("se solapa con")));
    }

    @Test
    void previewNoMarcaConflictoConBloquesDeOtroDia() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);
        when(bloqueRepo.findDelAnioConDetalle(1L)).thenReturn(List.of(bloqueExistente(
                asignacion, aula(), LocalTime.of(8, 0), LocalTime.of(9, 0))));

        PreviewCargaMasivaResponse respuesta = service.preview(List.of(csv(
                "1er Grado;A;Matemática;1234567;Aula 101;MARTES;08:00;09:00")), 1L);

        assertEquals("VALIDO", respuesta.bloques().get(0).estado());
    }

    @Test
    void confirmarInformaAvanceDuranteLaValidacionYElGuardado() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        // 250 bloques de 5 minutos que no se solapan (jornada 08:00-13:30 sin receso 11:00-11:30).
        List<ConfirmarCargaMasivaRequest.BloqueConfirmacion> bloques = new ArrayList<>();
        generar:
        for (DiaSemana dia : DiaSemana.values()) {
            for (int inicio = 8 * 60; inicio + 5 <= 13 * 60 + 30; inicio += 5) {
                if (inicio + 5 > 11 * 60 && inicio < 11 * 60 + 30) {
                    continue;
                }
                bloques.add(new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                        10L, 1L, dia,
                        LocalTime.of(inicio / 60, inicio % 60),
                        LocalTime.of((inicio + 5) / 60, (inicio + 5) % 60),
                        "fila-" + bloques.size()));
                if (bloques.size() == 250) {
                    break generar;
                }
            }
        }

        ProgresoGrabado progreso = new ProgresoGrabado();
        ConfirmarCargaMasivaResponse respuesta = service.confirmar(
                new ConfirmarCargaMasivaRequest(1L, bloques), 7L, progreso);

        assertEquals(250, respuesta.bloquesCreados());
        assertEquals(250, progreso.totalIniciado);
        assertEquals(List.of("VALIDANDO", "GUARDANDO", "FINALIZANDO"), progreso.fases);
        // 100 y 200 durante la validación, 0 al reiniciar la barra, 250 al guardar y al finalizar.
        assertEquals(List.of(100, 200, 0, 250, 250), progreso.procesados);
        verify(bloqueRepo, times(1)).insertarEnLotes(any());
    }

    @Test
    void confirmarConErroresDejaElAvanceEnElTotalDeFilas() {
        AnioAcademico anio = anio();
        AsignacionCurso asignacion = asignacion(anio);
        stubsPreview(anio, asignacion);

        ProgresoGrabado progreso = new ProgresoGrabado();
        service.confirmar(new ConfirmarCargaMasivaRequest(1L, List.of(
                new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                        10L, 1L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), "fila-1"),
                new ConfirmarCargaMasivaRequest.BloqueConfirmacion(
                        10L, 1L, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), "fila-2"))),
                7L, progreso);

        assertEquals(List.of(2), progreso.procesados);
        assertEquals("VALIDACION_CON_ERRORES", progreso.fases.get(progreso.fases.size() - 1));
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
        verify(bloqueRepo, never()).insertarEnLotes(any());
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
        private final List<Integer> procesados = new ArrayList<>();
        private final List<String> fases = new ArrayList<>();

        @Override
        public void iniciar(int total) {
            this.totalIniciado = total;
        }

        @Override
        public void actualizar(int procesados, int exitosos, int omitidos, int errores) {
            this.ultimoExitoso = exitosos;
            this.procesados.add(procesados);
        }

        @Override
        public void registrarError(String mensaje) {
        }

        @Override
        public void fase(String mensaje) {
            this.fases.add(mensaje);
        }
    }
}
