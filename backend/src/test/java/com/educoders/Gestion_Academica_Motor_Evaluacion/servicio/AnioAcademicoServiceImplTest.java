package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Bimestre;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.BimestreRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.BimestreResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.PeriodoMatriculaEstadoResponse;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoPeriodoMatricula;
import com.educoders.Infraestructura_Servicios.enums.NombreBimestre;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnioAcademicoServiceImplTest {

    @Mock private AnioAcademicoRepository anioRepo;
    @Mock private PeriodoMatriculaRepository periodoRepo;
    @Mock private BimestreRepository bimestreRepo;

    @InjectMocks private AnioAcademicoServiceImpl service;

    private AnioAcademico anio() {
        AnioAcademico a = new AnioAcademico();
        a.setId(1L);
        a.setAnio(2027);
        a.setFechaInicio(LocalDate.now().minusMonths(6));
        a.setFechaFin(LocalDate.now().plusMonths(6));
        return a;
    }

    private PeriodoMatricula periodo(LocalDate inicio, LocalDate fin) {
        PeriodoMatricula p = new PeriodoMatricula();
        p.setId(10L);
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        return p;
    }

    private Bimestre primerBimestre(LocalDate inicio) {
        Bimestre b = new Bimestre();
        b.setNombre(NombreBimestre.BIM_1);
        b.setFechaInicio(inicio);
        b.setFechaFin(inicio.plusMonths(2));
        return b;
    }

    @Test
    void periodoNoIniciado_devuelveEstadoYBanderas() {
        when(anioRepo.findById(1L)).thenReturn(Optional.of(anio()));
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(
                periodo(LocalDate.now().plusDays(10), LocalDate.now().plusMonths(2))));
        when(bimestreRepo.findFirstByNombreAndAnioAcademicoIdOrderByAnioAcademicoAnioDesc(
                NombreBimestre.BIM_1, 1L))
                .thenReturn(Optional.of(primerBimestre(LocalDate.now().plusMonths(3))));

        PeriodoMatriculaEstadoResponse r = service.obtenerPeriodoMatricula(1L);

        assertEquals(EstadoPeriodoMatricula.SIN_INICIAR, r.estado());
        assertTrue(r.asignacionCursoDocenteAbierta());
        assertFalse(r.crudBloquesHorarioAbierta());
        assertFalse(r.generacionSesionesAbierta());
    }

    @Test
    void periodoVigente_devuelveEstadoYBanderas() {
        when(anioRepo.findById(1L)).thenReturn(Optional.of(anio()));
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(
                periodo(LocalDate.now().minusDays(5), LocalDate.now().plusDays(20))));
        when(bimestreRepo.findFirstByNombreAndAnioAcademicoIdOrderByAnioAcademicoAnioDesc(
                NombreBimestre.BIM_1, 1L))
                .thenReturn(Optional.of(primerBimestre(LocalDate.now().plusDays(21))));

        PeriodoMatriculaEstadoResponse r = service.obtenerPeriodoMatricula(1L);

        assertEquals(EstadoPeriodoMatricula.VIGENTE, r.estado());
        assertFalse(r.asignacionCursoDocenteAbierta());
        assertTrue(r.crudBloquesHorarioAbierta());
        assertFalse(r.generacionSesionesAbierta());
    }

    @Test
    void periodoFinalizado_devuelveEstadoYBanderas() {
        when(anioRepo.findById(1L)).thenReturn(Optional.of(anio()));
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.of(
                periodo(LocalDate.now().minusMonths(3), LocalDate.now().minusDays(1))));
        when(bimestreRepo.findFirstByNombreAndAnioAcademicoIdOrderByAnioAcademicoAnioDesc(
                NombreBimestre.BIM_1, 1L))
                .thenReturn(Optional.of(primerBimestre(LocalDate.now())));

        PeriodoMatriculaEstadoResponse r = service.obtenerPeriodoMatricula(1L);

        assertEquals(EstadoPeriodoMatricula.FINALIZADO, r.estado());
        assertFalse(r.asignacionCursoDocenteAbierta());
        assertFalse(r.crudBloquesHorarioAbierta());
        assertTrue(r.generacionSesionesAbierta());
    }

    @Test
    void anioSinPeriodo_lanza404() {
        when(anioRepo.findById(1L)).thenReturn(Optional.of(anio()));
        when(periodoRepo.findByAnioAcademicoId(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtenerPeriodoMatricula(1L));
    }

    @Test
    void listarBimestres_devuelveLosBimestresDelAnio() {
        Bimestre primero = primerBimestre(LocalDate.of(2027, 3, 1));
        primero.setId(1L);
        Bimestre segundo = new Bimestre();
        segundo.setId(2L);
        segundo.setNombre(NombreBimestre.BIM_2);
        segundo.setFechaInicio(LocalDate.of(2027, 5, 16));
        segundo.setFechaFin(LocalDate.of(2027, 7, 30));

        when(anioRepo.findById(1L)).thenReturn(Optional.of(anio()));
        when(bimestreRepo.findByAnioAcademicoIdOrderByFechaInicioAsc(1L))
                .thenReturn(List.of(primero, segundo));

        List<BimestreResponse> respuesta = service.listarBimestres(1L);

        assertEquals(2, respuesta.size());
        assertEquals(NombreBimestre.BIM_1, respuesta.get(0).nombre());
        assertEquals(NombreBimestre.BIM_2, respuesta.get(1).nombre());
        assertEquals(LocalDate.of(2027, 3, 1), respuesta.get(0).fechaInicio());
    }

    @Test
    void listarBimestres_anioInexistente_lanza404() {
        when(anioRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.listarBimestres(99L));
    }
}
