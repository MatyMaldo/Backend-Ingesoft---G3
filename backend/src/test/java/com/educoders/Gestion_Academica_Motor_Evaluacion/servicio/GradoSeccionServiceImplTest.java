package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.GradoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearSeccionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.SeccionResponse;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;

import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;

import com.educoders.Infraestructura_Servicios.exception.ConflictoException;

import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.VacantesAnioResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.GradoConSeccionesResponse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
/**
 *
 * Pruebas unitarias del servicio de gestión de grados y secciones.
 */
@ExtendWith(MockitoExtension.class)
class GradoSeccionServiceImplTest {

    @Mock
    private GradoRepository gradoRepository;

    @Mock
    private SeccionRepository seccionRepository;

    @Mock
    private AnioAcademicoRepository anioAcademicoRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private PeriodoMatriculaRepository periodoMatriculaRepository;

    @Mock
    private MatriculaRepository matriculaRepository;

    @InjectMocks
    private GradoSeccionServiceImpl gradoSeccionService;

    @Test
    void verificarInicializacionDelServicio() {
        org.junit.jupiter.api.Assertions.assertNotNull(gradoSeccionService);
    }

    @Test
    void crearSeccionEnPrimerAnioAcademicoEnPreparacion() {
        LocalDate hoy = LocalDate.now();

        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anio = AnioAcademico.builder()
                .anio(hoy.getYear() + 1)
                .fechaInicio(hoy.plusMonths(3))
                .fechaFin(hoy.plusMonths(12))
                .build();
        anio.setId(2L);

        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(hoy.plusMonths(1));
        periodo.setFechaFin(hoy.plusMonths(2));

        CrearSeccionRequest request = new CrearSeccionRequest(
                "a", 1L, 2L
        );

        when(gradoRepository.findById(1L))
                .thenReturn(Optional.of(grado));

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anio));

        when(periodoMatriculaRepository.findByAnioAcademicoId(2L))
                .thenReturn(Optional.of(periodo));

        when(anioAcademicoRepository
                .findFirstByAnioLessThanOrderByAnioDesc(anio.getAnio()))
                .thenReturn(Optional.empty());

        when(seccionRepository.save(any(Seccion.class)))
                .thenAnswer(invocacion -> {
                    Seccion seccion = invocacion.getArgument(0);
                    seccion.setId(10L);
                    return seccion;
                });

        SeccionResponse resultado = gradoSeccionService.crearSeccion(request);

        ArgumentCaptor<Seccion> captor =
                ArgumentCaptor.forClass(Seccion.class);

        verify(seccionRepository).save(captor.capture());

        Seccion seccionGuardada = captor.getValue();

        assertEquals("A", seccionGuardada.getNombre());
        assertEquals(30, seccionGuardada.getCapacidadMaxima());
        assertNull(seccionGuardada.getTutor());
        assertEquals(1L, seccionGuardada.getGrado().getId());
        assertEquals(2L, seccionGuardada.getAnioAcademico().getId());
        assertEquals("A", resultado.getNombre());
    }


    @Test
    void crearSeccionEnAnioPosteriorEnPreparacion() {
        LocalDate hoy = LocalDate.now();

        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anioAnterior = AnioAcademico.builder()
                .anio(hoy.getYear())
                .fechaInicio(hoy.minusMonths(10))
                .fechaFin(hoy.minusDays(1))
                .build();
        anioAnterior.setId(1L);

        AnioAcademico anioNuevo = AnioAcademico.builder()
                .anio(hoy.getYear() + 1)
                .fechaInicio(hoy.plusMonths(3))
                .fechaFin(hoy.plusMonths(12))
                .build();
        anioNuevo.setId(2L);

        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(hoy.plusDays(10));
        periodo.setFechaFin(hoy.plusDays(30));

        CrearSeccionRequest request =
                new CrearSeccionRequest("b", 1L, 2L);

        when(gradoRepository.findById(1L))
                .thenReturn(Optional.of(grado));

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anioNuevo));

        when(periodoMatriculaRepository.findByAnioAcademicoId(2L))
                .thenReturn(Optional.of(periodo));

        when(anioAcademicoRepository
                .findFirstByAnioLessThanOrderByAnioDesc(anioNuevo.getAnio()))
                .thenReturn(Optional.of(anioAnterior));

        when(seccionRepository.save(any(Seccion.class)))
                .thenAnswer(invocacion -> {
                    Seccion seccion = invocacion.getArgument(0);
                    seccion.setId(11L);
                    return seccion;
                });

        SeccionResponse resultado =
                gradoSeccionService.crearSeccion(request);

        assertEquals("B", resultado.getNombre());
        assertEquals(30, resultado.getCapacidadMaxima());

        verify(seccionRepository).save(any(Seccion.class));
    }

    @Test
    void rechazarCreacionSeccionCuandoMatriculaYaInicio() {
        LocalDate hoy = LocalDate.now();

        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anio = AnioAcademico.builder()
                .anio(hoy.getYear() + 1)
                .fechaInicio(hoy.plusMonths(2))
                .fechaFin(hoy.plusMonths(12))
                .build();
        anio.setId(2L);

        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(hoy.minusDays(1));
        periodo.setFechaFin(hoy.plusDays(30));

        CrearSeccionRequest request =
                new CrearSeccionRequest("C", 1L, 2L);

        when(gradoRepository.findById(1L))
                .thenReturn(Optional.of(grado));

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anio));

        when(periodoMatriculaRepository.findByAnioAcademicoId(2L))
                .thenReturn(Optional.of(periodo));

        ReglaNegocioException excepcion = assertThrows(
                ReglaNegocioException.class,
                () -> gradoSeccionService.crearSeccion(request)
        );

        assertEquals(
                "El registro de secciones solo está habilitado mientras el año académico está en preparación",
                excepcion.getMessage()
        );

        verify(seccionRepository, never()).save(any(Seccion.class));
    }


    @Test
    void rechazarCreacionSeccionSiAnioAnteriorNoFinalizo() {
        LocalDate hoy = LocalDate.now();

        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anioAnterior = AnioAcademico.builder()
                .anio(hoy.getYear())
                .fechaInicio(hoy.minusMonths(3))
                .fechaFin(hoy.plusDays(20))
                .build();
        anioAnterior.setId(1L);

        AnioAcademico anioNuevo = AnioAcademico.builder()
                .anio(hoy.getYear() + 1)
                .fechaInicio(hoy.plusMonths(3))
                .fechaFin(hoy.plusMonths(12))
                .build();
        anioNuevo.setId(2L);

        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(hoy.plusDays(30));
        periodo.setFechaFin(hoy.plusDays(60));

        CrearSeccionRequest request =
                new CrearSeccionRequest("D", 1L, 2L);

        when(gradoRepository.findById(1L))
                .thenReturn(Optional.of(grado));

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anioNuevo));

        when(periodoMatriculaRepository.findByAnioAcademicoId(2L))
                .thenReturn(Optional.of(periodo));

        when(anioAcademicoRepository
                .findFirstByAnioLessThanOrderByAnioDesc(anioNuevo.getAnio()))
                .thenReturn(Optional.of(anioAnterior));

        ReglaNegocioException excepcion = assertThrows(
                ReglaNegocioException.class,
                () -> gradoSeccionService.crearSeccion(request)
        );

        assertEquals(
                "El registro de secciones solo está habilitado mientras el año académico está en preparación",
                excepcion.getMessage()
        );

        verify(seccionRepository, never()).save(any(Seccion.class));
    }



    @Test
    void rechazarCreacionSeccionSinPeriodoMatricula() {
        LocalDate hoy = LocalDate.now();

        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anio = AnioAcademico.builder()
                .anio(hoy.getYear() + 1)
                .fechaInicio(hoy.plusMonths(3))
                .fechaFin(hoy.plusMonths(12))
                .build();
        anio.setId(2L);

        CrearSeccionRequest request =
                new CrearSeccionRequest("E", 1L, 2L);

        when(gradoRepository.findById(1L))
                .thenReturn(Optional.of(grado));

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anio));

        when(periodoMatriculaRepository.findByAnioAcademicoId(2L))
                .thenReturn(Optional.empty());

        ReglaNegocioException excepcion = assertThrows(
                ReglaNegocioException.class,
                () -> gradoSeccionService.crearSeccion(request)
        );

        assertEquals(
                "El registro de secciones solo está habilitado mientras el año académico está en preparación",
                excepcion.getMessage()
        );

        verify(seccionRepository, never()).save(any(Seccion.class));
    }



    @Test
    void rechazarCreacionSeccionDuplicada() {
        LocalDate hoy = LocalDate.now();

        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anio = AnioAcademico.builder()
                .anio(hoy.getYear() + 1)
                .fechaInicio(hoy.plusMonths(3))
                .fechaFin(hoy.plusMonths(12))
                .build();
        anio.setId(2L);

        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(hoy.plusMonths(1));
        periodo.setFechaFin(hoy.plusMonths(2));

        CrearSeccionRequest request =
                new CrearSeccionRequest("a", 1L, 2L);

        when(gradoRepository.findById(1L))
                .thenReturn(Optional.of(grado));

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anio));

        when(periodoMatriculaRepository.findByAnioAcademicoId(2L))
                .thenReturn(Optional.of(periodo));

        when(anioAcademicoRepository
                .findFirstByAnioLessThanOrderByAnioDesc(anio.getAnio()))
                .thenReturn(Optional.empty());

        when(seccionRepository
                .existsByGradoIdAndAnioAcademicoIdAndNombreIgnoreCase(
                        1L, 2L, "a"))
                .thenReturn(true);

        assertThrows(
                ConflictoException.class,
                () -> gradoSeccionService.crearSeccion(request)
        );

        verify(seccionRepository, never()).save(any(Seccion.class));
    }



    @Test
    void rechazarCreacionSeccionElDiaDeCierreDelAnioAnterior() {
        LocalDate hoy = LocalDate.now();

        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anioAnterior = AnioAcademico.builder()
                .anio(hoy.getYear())
                .fechaInicio(hoy.minusMonths(3))
                .fechaFin(hoy)
                .build();
        anioAnterior.setId(1L);

        AnioAcademico anioNuevo = AnioAcademico.builder()
                .anio(hoy.getYear() + 1)
                .fechaInicio(hoy.plusMonths(3))
                .fechaFin(hoy.plusMonths(12))
                .build();
        anioNuevo.setId(2L);

        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(hoy.plusDays(10));
        periodo.setFechaFin(hoy.plusDays(30));

        CrearSeccionRequest request =
                new CrearSeccionRequest("F", 1L, 2L);

        when(gradoRepository.findById(1L))
                .thenReturn(Optional.of(grado));

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anioNuevo));

        when(periodoMatriculaRepository.findByAnioAcademicoId(2L))
                .thenReturn(Optional.of(periodo));

        when(anioAcademicoRepository
                .findFirstByAnioLessThanOrderByAnioDesc(anioNuevo.getAnio()))
                .thenReturn(Optional.of(anioAnterior));

        assertThrows(
                ReglaNegocioException.class,
                () -> gradoSeccionService.crearSeccion(request)
        );

        verify(seccionRepository, never()).save(any(Seccion.class));
    }


    @Test
    void rechazarCreacionSeccionElDiaDeInicioMatricula() {
        LocalDate hoy = LocalDate.now();

        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anio = AnioAcademico.builder()
                .anio(hoy.getYear() + 1)
                .fechaInicio(hoy.plusMonths(3))
                .fechaFin(hoy.plusMonths(12))
                .build();
        anio.setId(2L);

        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(hoy);
        periodo.setFechaFin(hoy.plusDays(30));

        CrearSeccionRequest request =
                new CrearSeccionRequest("G", 1L, 2L);

        when(gradoRepository.findById(1L))
                .thenReturn(Optional.of(grado));

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anio));

        when(periodoMatriculaRepository.findByAnioAcademicoId(2L))
                .thenReturn(Optional.of(periodo));

        ReglaNegocioException excepcion = assertThrows(
                ReglaNegocioException.class,
                () -> gradoSeccionService.crearSeccion(request)
        );

        assertEquals(
                "El registro de secciones solo está habilitado mientras el año académico está en preparación",
                excepcion.getMessage()
        );

        verify(seccionRepository, never()).save(any(Seccion.class));
    }


    @Test
    void permitirCreacionSeccionDiaAnteriorInicioMatricula() {
        LocalDate hoy = LocalDate.now();

        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anio = AnioAcademico.builder()
                .anio(hoy.getYear() + 1)
                .fechaInicio(hoy.plusMonths(3))
                .fechaFin(hoy.plusMonths(12))
                .build();
        anio.setId(2L);

        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(hoy.plusDays(1));
        periodo.setFechaFin(hoy.plusDays(30));

        CrearSeccionRequest request =
                new CrearSeccionRequest("H", 1L, 2L);

        when(gradoRepository.findById(1L))
                .thenReturn(Optional.of(grado));

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anio));

        when(periodoMatriculaRepository.findByAnioAcademicoId(2L))
                .thenReturn(Optional.of(periodo));

        when(anioAcademicoRepository
                .findFirstByAnioLessThanOrderByAnioDesc(anio.getAnio()))
                .thenReturn(Optional.empty());

        when(seccionRepository.save(any(Seccion.class)))
                .thenAnswer(invocacion -> {
                    Seccion seccion = invocacion.getArgument(0);
                    seccion.setId(12L);
                    return seccion;
                });

        SeccionResponse resultado =
                gradoSeccionService.crearSeccion(request);

        assertEquals("H", resultado.getNombre());
        assertEquals(30, resultado.getCapacidadMaxima());

        verify(seccionRepository).save(any(Seccion.class));
    }

    @Test
    void permitirCreacionSeccionDiaSiguienteCierreAnioAnterior() {
        LocalDate hoy = LocalDate.now();

        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anioAnterior = AnioAcademico.builder()
                .anio(hoy.getYear())
                .fechaInicio(hoy.minusMonths(10))
                .fechaFin(hoy.minusDays(1))
                .build();
        anioAnterior.setId(1L);

        AnioAcademico anioNuevo = AnioAcademico.builder()
                .anio(hoy.getYear() + 1)
                .fechaInicio(hoy.plusMonths(3))
                .fechaFin(hoy.plusMonths(12))
                .build();
        anioNuevo.setId(2L);

        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(hoy.plusDays(10));
        periodo.setFechaFin(hoy.plusDays(30));

        CrearSeccionRequest request =
                new CrearSeccionRequest("I", 1L, 2L);

        when(gradoRepository.findById(1L))
                .thenReturn(Optional.of(grado));

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anioNuevo));

        when(periodoMatriculaRepository.findByAnioAcademicoId(2L))
                .thenReturn(Optional.of(periodo));

        when(anioAcademicoRepository
                .findFirstByAnioLessThanOrderByAnioDesc(anioNuevo.getAnio()))
                .thenReturn(Optional.of(anioAnterior));

        when(seccionRepository.save(any(Seccion.class)))
                .thenAnswer(invocacion -> {
                    Seccion seccion = invocacion.getArgument(0);
                    seccion.setId(13L);
                    return seccion;
                });

        SeccionResponse resultado =
                gradoSeccionService.crearSeccion(request);

        assertEquals("I", resultado.getNombre());
        assertEquals(30, resultado.getCapacidadMaxima());

        verify(seccionRepository).save(any(Seccion.class));
    }


    @Test
    void permitirMismaLetraDeSeccionEnGradosDiferentes() {
        LocalDate hoy = LocalDate.now();

        Grado grado = Grado.builder()
                .nombre("Segundo")
                .build();
        grado.setId(2L);

        AnioAcademico anio = AnioAcademico.builder()
                .anio(hoy.getYear() + 1)
                .fechaInicio(hoy.plusMonths(3))
                .fechaFin(hoy.plusMonths(12))
                .build();
        anio.setId(3L);

        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(hoy.plusDays(10));
        periodo.setFechaFin(hoy.plusDays(30));

        CrearSeccionRequest request =
                new CrearSeccionRequest("A", 2L, 3L);

        when(gradoRepository.findById(2L))
                .thenReturn(Optional.of(grado));

        when(anioAcademicoRepository.findById(3L))
                .thenReturn(Optional.of(anio));

        when(periodoMatriculaRepository.findByAnioAcademicoId(3L))
                .thenReturn(Optional.of(periodo));

        when(anioAcademicoRepository
                .findFirstByAnioLessThanOrderByAnioDesc(anio.getAnio()))
                .thenReturn(Optional.empty());

        when(seccionRepository.save(any(Seccion.class)))
                .thenAnswer(invocacion -> {
                    Seccion seccion = invocacion.getArgument(0);
                    seccion.setId(14L);
                    return seccion;
                });

        SeccionResponse resultado =
                gradoSeccionService.crearSeccion(request);

        assertEquals("A", resultado.getNombre());
        assertEquals(30, resultado.getCapacidadMaxima());

        verify(seccionRepository)
                .existsByGradoIdAndAnioAcademicoIdAndNombreIgnoreCase(
                        2L, 3L, "A");

        verify(seccionRepository).save(any(Seccion.class));
    }



    @Test
    void calcularVacantesDisponiblesPorSeccion() {
        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anio = AnioAcademico.builder()
                .anio(2027)
                .build();
        anio.setId(2L);

        Seccion seccion = Seccion.builder()
                .nombre("A")
                .capacidadMaxima(30)
                .grado(grado)
                .anioAcademico(anio)
                .build();
        seccion.setId(3L);

        when(seccionRepository.findByGradoIdAndAnioAcademicoId(1L, 2L))
                .thenReturn(List.of(seccion));

        when(matriculaRepository.countBySeccionIdAndEstadoIn(
                3L,
                List.of(EstadoMatricula.CONFIRMADA)
        )).thenReturn(25L);

        List<SeccionResponse> resultado =
                gradoSeccionService.listarSeccionesPorGradoYAnio(1L, 2L);

        assertEquals(1, resultado.size());
        assertEquals(25L, resultado.get(0).getMatriculadosConfirmados());
        assertEquals(5L, resultado.get(0).getVacantesDisponibles());
        assertEquals(30, resultado.get(0).getCapacidadMaxima());

        verify(matriculaRepository).countBySeccionIdAndEstadoIn(
                3L,
                List.of(EstadoMatricula.CONFIRMADA)
        );
    }


    @Test
    void calcularVacantesCuandoSeccionEstaLlena() {
        Grado grado = Grado.builder()
                .nombre("Primero")
                .build();
        grado.setId(1L);

        AnioAcademico anio = AnioAcademico.builder()
                .anio(2027)
                .build();
        anio.setId(2L);

        Seccion seccion = Seccion.builder()
                .nombre("A")
                .capacidadMaxima(30)
                .grado(grado)
                .anioAcademico(anio)
                .build();
        seccion.setId(3L);

        when(seccionRepository.findByGradoIdAndAnioAcademicoId(1L, 2L))
                .thenReturn(List.of(seccion));

        when(matriculaRepository.countBySeccionIdAndEstadoIn(
                3L,
                List.of(EstadoMatricula.CONFIRMADA)
        )).thenReturn(30L);

        List<SeccionResponse> resultado =
                gradoSeccionService.listarSeccionesPorGradoYAnio(1L, 2L);

        assertEquals(1, resultado.size());
        assertEquals(30L, resultado.get(0).getMatriculadosConfirmados());
        assertEquals(0L, resultado.get(0).getVacantesDisponibles());
    }



    @Test
    void calcularVacantesTotalesDelAnio() {
        AnioAcademico anio = AnioAcademico.builder()
                .anio(2027)
                .build();
        anio.setId(2L);

        Seccion seccionA = Seccion.builder()
                .nombre("A")
                .capacidadMaxima(30)
                .build();
        seccionA.setId(10L);

        Seccion seccionB = Seccion.builder()
                .nombre("B")
                .capacidadMaxima(30)
                .build();
        seccionB.setId(11L);

        Seccion seccionC = Seccion.builder()
                .nombre("A")
                .capacidadMaxima(30)
                .build();
        seccionC.setId(12L);

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anio));

        when(seccionRepository.findByAnioAcademicoId(2L))
                .thenReturn(List.of(seccionA, seccionB, seccionC));

        List<EstadoMatricula> estados = List.of(
                EstadoMatricula.PENDIENTE,
                EstadoMatricula.CONFIRMADA
        );

        when(matriculaRepository.countBySeccionIdAndEstadoIn(10L, estados))
                .thenReturn(27L);

        when(matriculaRepository.countBySeccionIdAndEstadoIn(11L, estados))
                .thenReturn(23L);

        when(matriculaRepository.countBySeccionIdAndEstadoIn(12L, estados))
                .thenReturn(27L);

        VacantesAnioResponse resultado =
                gradoSeccionService.consultarVacantesAnio(2L);

        assertEquals(2L, resultado.anioAcademicoId());
        assertEquals(2027, resultado.anioAcademico());
        assertEquals(90L, resultado.capacidadTotal());
        assertEquals(77L, resultado.matriculasOcupantes());
        assertEquals(13L, resultado.vacantesDisponibles());

        verify(matriculaRepository).countBySeccionIdAndEstadoIn(10L, estados);
        verify(matriculaRepository).countBySeccionIdAndEstadoIn(11L, estados);
        verify(matriculaRepository).countBySeccionIdAndEstadoIn(12L, estados);
    }

    @Test
    void listarGradosConSeccionesIncluyeGradosSinSecciones() {
        AnioAcademico anio = AnioAcademico.builder()
                .anio(2027)
                .build();
        anio.setId(2L);

        Grado primero = Grado.builder()
                .nombre("Primero")
                .build();
        primero.setId(1L);

        Grado segundo = Grado.builder()
                .nombre("Segundo")
                .build();
        segundo.setId(2L);

        Seccion seccionA = Seccion.builder()
                .nombre("A")
                .capacidadMaxima(30)
                .grado(primero)
                .anioAcademico(anio)
                .build();
        seccionA.setId(10L);

        when(anioAcademicoRepository.findById(2L))
                .thenReturn(Optional.of(anio));

        when(gradoRepository.findAll())
                .thenReturn(List.of(segundo, primero));

        when(seccionRepository.findByGradoIdAndAnioAcademicoId(1L, 2L))
                .thenReturn(List.of(seccionA));

        when(seccionRepository.findByGradoIdAndAnioAcademicoId(2L, 2L))
                .thenReturn(List.of());

        when(matriculaRepository.countBySeccionIdAndEstadoIn(
                10L, List.of(EstadoMatricula.CONFIRMADA)))
                .thenReturn(25L);

        List<GradoConSeccionesResponse> resultado =
                gradoSeccionService.listarGradosConSecciones(2L);

        assertEquals(2, resultado.size());

        // Primero tiene una sección
        assertEquals("Primero", resultado.get(0).gradoNombre());
        assertEquals("Con secciones", resultado.get(0).estado());
        assertEquals(1, resultado.get(0).secciones().size());
        assertEquals("A", resultado.get(0).secciones().get(0).getNombre());
        assertEquals(25L, resultado.get(0).secciones().get(0).getMatriculadosConfirmados());
        assertEquals(5L, resultado.get(0).secciones().get(0).getVacantesDisponibles());

        // Segundo no tiene secciones
        assertEquals("Segundo", resultado.get(1).gradoNombre());
        assertEquals("Sin secciones", resultado.get(1).estado());
        assertTrue(resultado.get(1).secciones().isEmpty());
    }


}