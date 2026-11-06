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
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.persistencia.AulaRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.CargaMasivaResponse;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorarioCsvServiceImplTest {

    @Mock private PeriodoMatriculaRepository periodoRepo;
    @Mock private AnioAcademicoRepository anioRepo;
    @Mock private GradoRepository gradoRepo;
    @Mock private SeccionRepository seccionRepo;
    @Mock private CursoRepository cursoRepo;
    @Mock private DocenteRepository docenteRepo;
    @Mock private AulaRepository aulaRepo;
    @Mock private AsignacionCursoRepository asignacionRepo;
    @Mock private BloqueHorarioRepository bloqueRepo;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private HorarioCsvServiceImpl service;

    private static final String CABECERA =
            "Grado;Sección;Curso;DNI Docente;Aula;Día;Hora inicio;Hora fin";
    private static final String GRADO_NOMBRE = "1er Grado de Secundaria";

    // --- datos de prueba ---

    private AnioAcademico crearAnio() {
        AnioAcademico a = new AnioAcademico();
        a.setId(1L);
        a.setAnio(2027);
        a.setFechaInicio(LocalDate.now().plusMonths(3));
        a.setFechaFin(LocalDate.now().plusMonths(12));
        return a;
    }

    private PeriodoMatricula crearPeriodo(boolean abierto) {
        PeriodoMatricula p = new PeriodoMatricula();
        p.setId(1L);
        p.setFechaInicio(LocalDate.now().minusMonths(1));
        p.setFechaFin(abierto ? LocalDate.now().plusMonths(2) : LocalDate.now().minusDays(1));
        return p;
    }

    private Grado crearGrado() {
        Grado g = new Grado();
        g.setId(1L);
        g.setNombre(GRADO_NOMBRE);
        return g;
    }

    private Seccion crearSeccion() {
        Seccion s = new Seccion();
        s.setId(1L);
        s.setNombre("A");
        s.setGrado(crearGrado());
        s.setAnioAcademico(crearAnio());
        return s;
    }

    private Curso crearCurso() {
        Curso c = new Curso();
        c.setId(1L);
        c.setNombre("Matemática");
        return c;
    }

    private Docente crearDocente(Long id, String dni) {
        Docente d = new Docente();
        d.setId(id);
        d.setDni(dni);
        d.setNombre("Prof");
        d.setApellidoPaterno("Test");
        d.setActivo(true);
        return d;
    }

    private Aula crearAula() {
        Aula a = new Aula();
        a.setId(1L);
        a.setNombre("Aula 101");
        a.setCapacidad(30);
        return a;
    }

    private MockMultipartFile csv(String contenido) {
        return new MockMultipartFile("file", "horarios.csv", "text/csv",
                contenido.getBytes(StandardCharsets.UTF_8));
    }

    private void stubsBase(AnioAcademico anio, Docente docente, Aula aula) {
        lenient().when(anioRepo.findById(1L)).thenReturn(Optional.of(anio));
        lenient().when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodo(true)));
        lenient().when(gradoRepo.findByNombre(GRADO_NOMBRE)).thenReturn(Optional.of(crearGrado()));
        lenient().when(gradoRepo.findById(1L)).thenReturn(Optional.of(crearGrado()));
        lenient().when(seccionRepo.findById(1L)).thenReturn(Optional.of(crearSeccion()));
        lenient().when(seccionRepo.findByGradoIdAndAnioAcademicoIdAndNombre(1L, 1L, "A"))
                .thenReturn(Optional.of(crearSeccion()));
        lenient().when(cursoRepo.findByNombre("Matemática")).thenReturn(Optional.of(crearCurso()));
        lenient().when(docenteRepo.findByDni(docente.getDni())).thenReturn(Optional.of(docente));
        lenient().when(aulaRepo.findByNombre(aula.getNombre())).thenReturn(Optional.of(aula));
        lenient().when(asignacionRepo.findByCursoIdAndSeccionId(1L, 1L)).thenReturn(Optional.empty());
        lenient().when(bloqueRepo.findConflictoDocente(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of());
        lenient().when(bloqueRepo.findConflictoSeccion(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of());
        lenient().when(bloqueRepo.findConflictoAula(any(), any(), any(), anyLong(), any(), anyLong()))
                .thenReturn(List.of());
        lenient().when(asignacionRepo.save(any(AsignacionCurso.class))).thenAnswer(inv -> {
            AsignacionCurso a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });
        lenient().when(bloqueRepo.save(any(BloqueHorario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- HU-GEMIH-08 ---

    @Test
    void generarPlantilla_incluyeBomUtf8() {
        byte[] bytes = service.generarPlantilla();
        assertTrue(bytes.length >= 3);
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
    }

    @Test
    void generarPlantilla_encabezadosEnOrden() {
        String texto = new String(service.generarPlantilla(), StandardCharsets.UTF_8);
        String sinBom = texto.startsWith("\uFEFF") ? texto.substring(1) : texto;
        String encabezado = sinBom.split("\\r?\\n")[0];
        assertEquals(CABECERA, encabezado);
    }

    @Test
    void generarPlantilla_usaSeparadorPuntoYComa() {
        String texto = new String(service.generarPlantilla(), StandardCharsets.UTF_8);
        String encabezado = texto.replace("\uFEFF", "").split("\\r?\\n")[0];
        assertEquals(7, encabezado.chars().filter(c -> c == ';').count());
    }

    @Test
    void generarPlantilla_incluyeFilaEjemplo() {
        String texto = new String(service.generarPlantilla(), StandardCharsets.UTF_8);
        String[] lineas = texto.replace("\uFEFF", "").split("\\r?\\n");
        assertTrue(lineas.length >= 2);
        assertEquals("EJEMPLO;A;Matemática;1234567;101;Lunes;08:00;09:00", lineas[1]);
    }

    // --- HU-GEMIH-09 ---

    @Test
    void cargaMasiva_exitosa() {
        AnioAcademico anio = crearAnio();
        Docente docente = crearDocente(2L, "1234567");
        Aula aula = crearAula();
        stubsBase(anio, docente, aula);

        String contenido = CABECERA + "\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 101;Lunes;08:00;09:00\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 101;Martes;10:00;11:00\n";

        CargaMasivaResponse r = service.cargarMasivo(csv(contenido), 1L);

        assertEquals(2, r.getFilasProcesadas());
        assertEquals(2, r.getBloquesCreados());
        assertTrue(r.getErrores().isEmpty());
        verify(bloqueRepo, org.mockito.Mockito.times(2)).save(any(BloqueHorario.class));
    }

    @Test
    void cargaMasiva_conFiltroGrado_exitosa() {
        AnioAcademico anio = crearAnio();
        Docente docente = crearDocente(2L, "1234567");
        Aula aula = crearAula();
        stubsBase(anio, docente, aula);

        String contenido = CABECERA + "\n"
                + ";A;Matemática;1234567;Aula 101;Lunes;08:00;09:00\n";

        CargaMasivaResponse r = service.cargarMasivo(csv(contenido), 1L, 1L, null);

        assertEquals(1, r.getBloquesCreados());
        assertTrue(r.getErrores().isEmpty());
    }

    @Test
    void cargaMasiva_conFiltroSeccion_exitosa() {
        AnioAcademico anio = crearAnio();
        Docente docente = crearDocente(2L, "1234567");
        Aula aula = crearAula();
        stubsBase(anio, docente, aula);

        String contenido = CABECERA + "\n"
                + ";;Matemática;1234567;Aula 101;Lunes;08:00;09:00\n";

        CargaMasivaResponse r = service.cargarMasivo(csv(contenido), 1L, null, 1L);

        assertEquals(1, r.getBloquesCreados());
        assertTrue(r.getErrores().isEmpty());
    }

    @Test
    void cargaMasiva_filtroGradoIncompatible_rechazaFila() {
        AnioAcademico anio = crearAnio();
        Docente docente = crearDocente(2L, "1234567");
        Aula aula = crearAula();
        stubsBase(anio, docente, aula);

        Grado otroGrado = new Grado();
        otroGrado.setId(2L);
        otroGrado.setNombre("2do Grado");
        lenient().when(gradoRepo.findById(2L)).thenReturn(Optional.of(otroGrado));

        String contenido = CABECERA + "\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 101;Lunes;08:00;09:00\n";

        CargaMasivaResponse r = service.cargarMasivo(csv(contenido), 1L, 2L, null);

        assertEquals(0, r.getBloquesCreados());
        assertEquals(1, r.getErrores().size());
        assertTrue(r.getErrores().get(0).getMensaje().contains("grado distinto"));
    }

    @Test
    void cargaMasiva_filtrosInconsistentes_rechaza() {
        AnioAcademico anio = crearAnio();
        Docente docente = crearDocente(2L, "1234567");
        Aula aula = crearAula();
        stubsBase(anio, docente, aula);

        Grado otroGrado = new Grado();
        otroGrado.setId(2L);
        otroGrado.setNombre("2do Grado");

        Seccion otraSeccion = new Seccion();
        otraSeccion.setId(2L);
        otraSeccion.setNombre("B");
        otraSeccion.setGrado(otroGrado);
        otraSeccion.setAnioAcademico(anio);
        lenient().when(seccionRepo.findById(2L)).thenReturn(Optional.of(otraSeccion));

        String contenido = CABECERA + "\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 101;Lunes;08:00;09:00\n";

        assertThrows(ReglaNegocioException.class,
                () -> service.cargarMasivo(csv(contenido), 1L, 1L, 2L));
    }

    @Test
    void cargaMasiva_periodoNoIniciado_rechaza() {
        AnioAcademico anio = crearAnio();
        Docente docente = crearDocente(2L, "1234567");
        Aula aula = crearAula();
        stubsBase(anio, docente, aula);

        PeriodoMatricula periodoNoIniciado = new PeriodoMatricula();
        periodoNoIniciado.setId(3L);
        periodoNoIniciado.setFechaInicio(LocalDate.now().plusDays(10));
        periodoNoIniciado.setFechaFin(LocalDate.now().plusMonths(3));
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(periodoNoIniciado));

        String contenido = CABECERA + "\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 101;Lunes;08:00;09:00\n";

        assertThrows(ReglaNegocioException.class,
                () -> service.cargarMasivo(csv(contenido), 1L));
    }

    @Test
    void cargaMasiva_todoONada() {
        AnioAcademico anio = crearAnio();
        Docente docente = crearDocente(2L, "1234567");
        Aula aula = crearAula();
        stubsBase(anio, docente, aula);
        when(aulaRepo.findByNombre("Aula 999")).thenReturn(Optional.empty());

        String contenido = CABECERA + "\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 101;Lunes;08:00;09:00\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 999;Martes;10:00;11:00\n";

        CargaMasivaResponse r = service.cargarMasivo(csv(contenido), 1L);

        assertEquals(0, r.getBloquesCreados());
        assertEquals(1, r.getErrores().size());
        assertEquals(3, r.getErrores().get(0).getFila());
        verify(bloqueRepo, never()).save(any(BloqueHorario.class));
    }

    @Test
    void cargaMasiva_encabezadoInvalido() {
        String contenido = "Grado;Seccion;Curso;DNI;Aula;Dia;Inicio;Fin\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 101;Lunes;08:00;09:00\n";

        assertThrows(ReglaNegocioException.class,
                () -> service.cargarMasivo(csv(contenido), 1L));
    }

    @Test
    void cargaMasiva_filaEjemplo() {
        AnioAcademico anio = crearAnio();
        Docente docente = crearDocente(2L, "1234567");
        Aula aula = crearAula();
        stubsBase(anio, docente, aula);

        String contenido = CABECERA + "\n"
                + "EJEMPLO;A;Matemática;1234567;Aula 101;Lunes;08:00;09:00\n";

        CargaMasivaResponse r = service.cargarMasivo(csv(contenido), 1L);

        assertEquals(0, r.getBloquesCreados());
        assertEquals(1, r.getErrores().size());
        assertTrue(r.getErrores().get(0).getMensaje().contains("EJEMPLO"));
    }

    @Test
    void cargaMasiva_programacionCerrada() {
        AnioAcademico anio = crearAnio();
        Docente docente = crearDocente(2L, "1234567");
        Aula aula = crearAula();
        stubsBase(anio, docente, aula);
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodo(false)));

        String contenido = CABECERA + "\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 101;Lunes;08:00;09:00\n";

        assertThrows(ReglaNegocioException.class,
                () -> service.cargarMasivo(csv(contenido), 1L));
    }

    @Test
    void cargaMasiva_conflictoDocenteEnArchivo() {
        AnioAcademico anio = crearAnio();
        Docente docente = crearDocente(2L, "1234567");
        Aula aula = crearAula();
        stubsBase(anio, docente, aula);

        String contenido = CABECERA + "\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 101;Lunes;08:00;10:00\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 101;Lunes;09:00;11:00\n";

        CargaMasivaResponse r = service.cargarMasivo(csv(contenido), 1L);

        assertEquals(0, r.getBloquesCreados());
        assertEquals(1, r.getErrores().size());
        assertEquals(3, r.getErrores().get(0).getFila());
    }

    @Test
    void cargaMasiva_horaInvalida() {
        AnioAcademico anio = crearAnio();
        Docente docente = crearDocente(2L, "1234567");
        Aula aula = crearAula();
        stubsBase(anio, docente, aula);

        String contenido = CABECERA + "\n"
                + GRADO_NOMBRE + ";A;Matemática;1234567;Aula 101;Lunes;10:00;09:00\n";

        CargaMasivaResponse r = service.cargarMasivo(csv(contenido), 1L);

        assertEquals(0, r.getBloquesCreados());
        assertEquals(1, r.getErrores().size());
        assertTrue(r.getErrores().get(0).getMensaje().contains("posterior"));
    }

    @Test
    void cargaMasiva_cursoConOtroDocente() {
        AnioAcademico anio = crearAnio();
        Docente uno = crearDocente(2L, "11111111");
        Docente dos = crearDocente(3L, "22222222");
        Aula aula = crearAula();
        stubsBase(anio, uno, aula);
        lenient().when(docenteRepo.findByDni("22222222")).thenReturn(Optional.of(dos));

        String contenido = CABECERA + "\n"
                + GRADO_NOMBRE + ";A;Matemática;11111111;Aula 101;Lunes;08:00;09:00\n"
                + GRADO_NOMBRE + ";A;Matemática;22222222;Aula 101;Martes;08:00;09:00\n";

        CargaMasivaResponse r = service.cargarMasivo(csv(contenido), 1L);

        assertEquals(0, r.getBloquesCreados());
        assertEquals(1, r.getErrores().size());
    }

    @Test
    void cargaMasiva_masDe1000Filas() {
        StringBuilder sb = new StringBuilder(CABECERA).append("\n");
        for (int i = 0; i < 1001; i++) {
            sb.append(GRADO_NOMBRE)
                    .append(";A;Matemática;1234567;Aula 101;Lunes;08:00;09:00\n");
        }

        assertThrows(ReglaNegocioException.class,
                () -> service.cargarMasivo(csv(sb.toString()), 1L));
    }

    @Test
    void cargaMasiva_extensionInvalida() {
        MockMultipartFile archivo = new MockMultipartFile("file", "horarios.txt", "text/plain",
                "contenido".getBytes(StandardCharsets.UTF_8));

        assertThrows(ReglaNegocioException.class,
                () -> service.cargarMasivo(archivo, 1L));
    }
}