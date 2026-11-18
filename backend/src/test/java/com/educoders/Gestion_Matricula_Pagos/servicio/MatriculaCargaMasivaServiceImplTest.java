package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.GradoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ConfirmarCargaMasivaMatriculasRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ConfirmarCargaMasivaMatriculasResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.PreviewCargaMasivaMatriculasResponse;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatriculaCargaMasivaServiceImplTest {

    @Mock private MatriculaRepository matriculaRepository;
    @Mock private PeriodoMatriculaRepository periodoMatriculaRepository;
    @Mock private SeccionRepository seccionRepository;
    @Mock private GradoRepository gradoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private AnioAcademicoRepository anioAcademicoRepository;
    @Mock private MatriculaArchivoParser archivoParser;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private MatriculaCargaMasivaServiceImpl service;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarAdmin() {
        Administrador admin = new Administrador();
        admin.setId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    private AnioAcademico crearAnio() {
        AnioAcademico anio = new AnioAcademico();
        anio.setId(1L);
        anio.setAnio(2027);
        anio.setFechaInicio(LocalDate.now().minusMonths(2));
        anio.setFechaFin(LocalDate.now().plusMonths(10));
        return anio;
    }

    private PeriodoMatricula crearPeriodo(AnioAcademico anio, boolean vigente) {
        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(vigente
                ? LocalDate.now().minusDays(10)
                : LocalDate.now().minusDays(60));
        periodo.setFechaFin(vigente
                ? LocalDate.now().plusDays(20)
                : LocalDate.now().minusDays(1));
        periodo.setAnioAcademico(anio);
        return periodo;
    }

    private Grado crearGrado() {
        Grado grado = new Grado();
        grado.setId(1L);
        grado.setNombre("1er Grado de Secundaria");
        return grado;
    }

    private Seccion crearSeccion(Grado grado, AnioAcademico anio) {
        Seccion seccion = new Seccion();
        seccion.setId(1L);
        seccion.setNombre("A");
        seccion.setCapacidadMaxima(30);
        seccion.setGrado(grado);
        seccion.setAnioAcademico(anio);
        return seccion;
    }

    private Estudiante crearEstudiante() {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(5L);
        estudiante.setNombre("Luis");
        estudiante.setApellidoPaterno("Ramirez");
        estudiante.setApellidoMaterno("Torres");
        estudiante.setCodigo("EST-001");
        estudiante.setDni("12345678");
        estudiante.setActivo(true);
        return estudiante;
    }

    private MultipartFile crearArchivo() {
        MultipartFile archivo = org.mockito.Mockito.mock(MultipartFile.class);
        lenient().when(archivo.getOriginalFilename()).thenReturn("matriculas.csv");
        lenient().when(archivo.isEmpty()).thenReturn(false);
        lenient().when(archivo.getSize()).thenReturn(1024L);
        return archivo;
    }

    private void prepararContextoPreview(
            AnioAcademico anio,
            PeriodoMatricula periodo,
            Grado grado,
            Seccion seccion,
            Estudiante estudiante) {

        when(anioAcademicoRepository.findById(anio.getId())).thenReturn(Optional.of(anio));
        when(periodoMatriculaRepository.findByAnioAcademicoId(anio.getId()))
                .thenReturn(Optional.of(periodo));
        when(gradoRepository.findAll()).thenReturn(List.of(grado));
        when(seccionRepository.findByAnioAcademicoId(anio.getId()))
                .thenReturn(List.of(seccion));
        when(estudianteRepository.buscarPorDnis(any())).thenReturn(List.of(estudiante));
        when(matriculaRepository.buscarPorEstudiantesYAnio(any(), eq(anio.getId())))
                .thenReturn(List.of());
        when(matriculaRepository.contarPorSecciones(any(), any())).thenReturn(List.of());
    }

    @Test
    void preview_archivoValido_devuelveFilaValidaYSecciones() {
        autenticarAdmin();

        AnioAcademico anio = crearAnio();
        PeriodoMatricula periodo = crearPeriodo(anio, true);
        Grado grado = crearGrado();
        Seccion seccion = crearSeccion(grado, anio);
        Estudiante estudiante = crearEstudiante();
        MultipartFile archivo = crearArchivo();

        prepararContextoPreview(anio, periodo, grado, seccion, estudiante);
        when(archivoParser.leer(archivo)).thenReturn(List.of(
                new MatriculaArchivoParser.FilaMatricula(
                        2, "12345678", "", "2027",
                        "1er Grado de Secundaria", "A")));

        PreviewCargaMasivaMatriculasResponse respuesta =
                service.preview(archivo, anio.getId());

        assertEquals(1, respuesta.resumen().validas());
        assertEquals(0, respuesta.resumen().conError());
        assertEquals("VALIDO", respuesta.filas().get(0).estado());
        assertEquals(5L, respuesta.filas().get(0).estudianteId());
        assertEquals(1L, respuesta.filas().get(0).seccionId());
        assertEquals(1, respuesta.secciones().size());
        assertEquals(30, respuesta.secciones().get(0).vacantes());
    }

    @Test
    void preview_estudianteRepetido_marcaDuplicado() {
        autenticarAdmin();

        AnioAcademico anio = crearAnio();
        PeriodoMatricula periodo = crearPeriodo(anio, true);
        Grado grado = crearGrado();
        Seccion seccion = crearSeccion(grado, anio);
        Estudiante estudiante = crearEstudiante();
        MultipartFile archivo = crearArchivo();

        prepararContextoPreview(anio, periodo, grado, seccion, estudiante);
        when(archivoParser.leer(archivo)).thenReturn(List.of(
                new MatriculaArchivoParser.FilaMatricula(
                        2, "12345678", "", "2027",
                        "1er Grado de Secundaria", "A"),
                new MatriculaArchivoParser.FilaMatricula(
                        3, "12345678", "", "2027",
                        "1er Grado de Secundaria", "A")));

        PreviewCargaMasivaMatriculasResponse respuesta =
                service.preview(archivo, anio.getId());

        assertEquals("VALIDO", respuesta.filas().get(0).estado());
        assertEquals("DUPLICADO", respuesta.filas().get(1).estado());
        assertEquals(1, respuesta.resumen().duplicadas());
    }

    @Test
    void preview_matriculaExistente_marcaConflicto() {
        autenticarAdmin();

        AnioAcademico anio = crearAnio();
        PeriodoMatricula periodo = crearPeriodo(anio, true);
        Grado grado = crearGrado();
        Seccion seccion = crearSeccion(grado, anio);
        Estudiante estudiante = crearEstudiante();
        MultipartFile archivo = crearArchivo();

        Matricula existente = new Matricula();
        existente.setId(50L);
        existente.setEstado(EstadoMatricula.PENDIENTE);
        existente.setEstudiante(estudiante);
        existente.setSeccion(seccion);

        when(anioAcademicoRepository.findById(anio.getId())).thenReturn(Optional.of(anio));
        when(periodoMatriculaRepository.findByAnioAcademicoId(anio.getId()))
                .thenReturn(Optional.of(periodo));
        when(gradoRepository.findAll()).thenReturn(List.of(grado));
        when(seccionRepository.findByAnioAcademicoId(anio.getId()))
                .thenReturn(List.of(seccion));
        when(estudianteRepository.buscarPorDnis(any())).thenReturn(List.of(estudiante));
        when(matriculaRepository.buscarPorEstudiantesYAnio(any(), eq(anio.getId())))
                .thenReturn(List.of(existente));
        when(matriculaRepository.contarPorSecciones(any(), any())).thenReturn(List.of());
        when(archivoParser.leer(archivo)).thenReturn(List.of(
                new MatriculaArchivoParser.FilaMatricula(
                        2, "12345678", "", "2027",
                        "1er Grado de Secundaria", "A")));

        PreviewCargaMasivaMatriculasResponse respuesta =
                service.preview(archivo, anio.getId());

        assertEquals("CONFLICTO", respuesta.filas().get(0).estado());
        assertTrue(respuesta.filas().get(0).errores().get(0)
                .contains("ya tiene una matrícula PENDIENTE"));
    }

    @Test
    void preview_periodoVencido_rechaza() {
        autenticarAdmin();

        AnioAcademico anio = crearAnio();
        PeriodoMatricula periodo = crearPeriodo(anio, false);

        when(anioAcademicoRepository.findById(anio.getId())).thenReturn(Optional.of(anio));
        when(periodoMatriculaRepository.findByAnioAcademicoId(anio.getId()))
                .thenReturn(Optional.of(periodo));

        assertThrows(ReglaNegocioException.class,
                () -> service.preview(crearArchivo(), anio.getId()));
    }

    @Test
    void confirmar_exitoso_creaMatriculaPendiente() {
        autenticarAdmin();

        AnioAcademico anio = crearAnio();
        PeriodoMatricula periodo = crearPeriodo(anio, true);
        Grado grado = crearGrado();
        Seccion seccion = crearSeccion(grado, anio);
        Estudiante estudiante = crearEstudiante();

        when(anioAcademicoRepository.findById(anio.getId())).thenReturn(Optional.of(anio));
        when(periodoMatriculaRepository.findByAnioAcademicoId(anio.getId()))
                .thenReturn(Optional.of(periodo));
        when(estudianteRepository.findAllById(any())).thenReturn(List.of(estudiante));
        when(seccionRepository.findAllById(any())).thenReturn(List.of(seccion));
        when(matriculaRepository.buscarPorEstudiantesYAnio(any(), eq(anio.getId())))
                .thenReturn(List.of());
        when(matriculaRepository.contarPorSecciones(any(), any())).thenReturn(List.of());

        ConfirmarCargaMasivaMatriculasResponse respuesta = service.confirmar(
                new ConfirmarCargaMasivaMatriculasRequest(
                        anio.getId(),
                        List.of(new ConfirmarCargaMasivaMatriculasRequest.FilaConfirmacion(
                                "p-1", 5L, anio.getId(), 1L, 1L,
                                "matriculas.csv#2"))));

        assertEquals(1, respuesta.creados());
        assertTrue(respuesta.errores().isEmpty());

        ArgumentCaptor<Iterable<Matricula>> captor =
                ArgumentCaptor.forClass((Class) Iterable.class);
        verify(matriculaRepository).saveAll(captor.capture());
        assertEquals(EstadoMatricula.PENDIENTE, captor.getValue().iterator().next().getEstado());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void confirmar_estudianteInexistente_noCreaNada() {
        autenticarAdmin();

        AnioAcademico anio = crearAnio();
        PeriodoMatricula periodo = crearPeriodo(anio, true);

        when(anioAcademicoRepository.findById(anio.getId())).thenReturn(Optional.of(anio));
        when(periodoMatriculaRepository.findByAnioAcademicoId(anio.getId()))
                .thenReturn(Optional.of(periodo));
        when(estudianteRepository.findAllById(any())).thenReturn(List.of());
        when(seccionRepository.findAllById(any())).thenReturn(List.of());
        when(matriculaRepository.contarPorSecciones(any(), any())).thenReturn(List.of());

        ConfirmarCargaMasivaMatriculasResponse respuesta = service.confirmar(
                new ConfirmarCargaMasivaMatriculasRequest(
                        anio.getId(),
                        List.of(new ConfirmarCargaMasivaMatriculasRequest.FilaConfirmacion(
                                "p-1", 99L, anio.getId(), 1L, 1L,
                                "matriculas.csv#2"))));

        assertEquals(0, respuesta.creados());
        assertEquals(1, respuesta.errores().size());
        verify(matriculaRepository, never()).saveAll(any());
    }

    @Test
    void confirmar_estudianteRepetido_devuelveError() {
        autenticarAdmin();

        AnioAcademico anio = crearAnio();
        PeriodoMatricula periodo = crearPeriodo(anio, true);
        Grado grado = crearGrado();
        Seccion seccion = crearSeccion(grado, anio);
        Estudiante estudiante = crearEstudiante();

        when(anioAcademicoRepository.findById(anio.getId())).thenReturn(Optional.of(anio));
        when(periodoMatriculaRepository.findByAnioAcademicoId(anio.getId()))
                .thenReturn(Optional.of(periodo));
        when(estudianteRepository.findAllById(any())).thenReturn(List.of(estudiante));
        when(seccionRepository.findAllById(any())).thenReturn(List.of(seccion));
        when(matriculaRepository.buscarPorEstudiantesYAnio(any(), eq(anio.getId())))
                .thenReturn(List.of());
        when(matriculaRepository.contarPorSecciones(any(), any())).thenReturn(List.of());

        ConfirmarCargaMasivaMatriculasResponse respuesta = service.confirmar(
                new ConfirmarCargaMasivaMatriculasRequest(
                        anio.getId(),
                        List.of(
                                new ConfirmarCargaMasivaMatriculasRequest.FilaConfirmacion(
                                        "p-1", 5L, anio.getId(), 1L, 1L,
                                        "matriculas.csv#2"),
                                new ConfirmarCargaMasivaMatriculasRequest.FilaConfirmacion(
                                        "p-2", 5L, anio.getId(), 1L, 1L,
                                        "matriculas.csv#3"))));

        assertEquals(0, respuesta.creados());
        assertEquals(1, respuesta.errores().size());
        assertTrue(respuesta.errores().get(0).mensaje().contains("repetido"));
        verify(matriculaRepository, never()).saveAll(any());
    }

    @Test
    void generarPlantilla_csvYxlsx() {
        autenticarAdmin();

        byte[] csv = service.generarPlantilla("csv");
        String contenido = new String(csv, StandardCharsets.UTF_8);
        assertTrue(contenido.startsWith("\uFEFF"));
        assertTrue(contenido.contains("DNI;Código;Año;Grado;Sección"));

        byte[] xlsx = service.generarPlantilla("xlsx");
        assertTrue(xlsx.length > 0);
    }

    @Test
    void exportar_csv_incluyeSoloFormatoDeImportacion() {
        autenticarAdmin();

        AnioAcademico anio = crearAnio();
        Grado grado = crearGrado();
        Seccion seccion = crearSeccion(grado, anio);
        Estudiante estudiante = crearEstudiante();

        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setEstado(EstadoMatricula.CONFIRMADA);
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);

        when(anioAcademicoRepository.findById(anio.getId())).thenReturn(Optional.of(anio));
        when(matriculaRepository.buscarPorAnioYEstadosConDetalle(eq(anio.getId()), any()))
                .thenReturn(List.of(matricula));

        String contenido = new String(
                service.exportar(anio.getId(), "csv"), StandardCharsets.UTF_8);

        assertTrue(contenido.contains(
                "12345678;EST-001;2027;1er Grado de Secundaria;A"));
        assertTrue(!contenido.contains("CONFIRMADA"));
    }
}
