package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.CompromisoPagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoPagoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoPensionRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosMatriculaLoteRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosMatriculaLoteResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosPensionLoteRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosPensionLoteResponse;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.Mes;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.servicio.ServicioNotificacionEmail;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompromisoPagoServiceImplTest {

    @Mock private CompromisoPagoRepository compromisoPagoRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private PeriodoMatriculaRepository periodoMatriculaRepository;
    @Mock private AnioAcademicoRepository anioAcademicoRepository;
    @Mock private ServicioNotificacionEmail servicioNotificacionEmail;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private CompromisoPagoServiceImpl service;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Administrador crearAdmin() {
        Administrador a = new Administrador();
        a.setId(1L);
        return a;
    }

    private Apoderado crearApoderado() {
        Apoderado a = new Apoderado();
        a.setId(4L);
        a.setNombre("Carlos");
        a.setApellidoPaterno("Ramirez");
        a.setEmail("carlos@gmail.com");
        return a;
    }

    private Estudiante crearEstudiante(Apoderado apoderado) {
        Estudiante e = new Estudiante();
        e.setId(5L);
        e.setNombre("Luis");
        e.setApellidoPaterno("Ramirez");
        e.setApoderado(apoderado);
        return e;
    }

    private Seccion crearSeccion(boolean anioVigente) {
        AnioAcademico anio = new AnioAcademico();
        anio.setId(1L);
        anio.setAnio(2027);
        anio.setFechaInicio(anioVigente ? LocalDate.now().minusMonths(1) : LocalDate.now().plusMonths(6));
        anio.setFechaFin(anioVigente ? LocalDate.now().plusMonths(6) : LocalDate.now().plusMonths(12));

        Grado g = new Grado();
        g.setId(1L);
        g.setNombre("1er Grado");

        Seccion s = new Seccion();
        s.setId(1L);
        s.setNombre("A");
        s.setGrado(g);
        s.setAnioAcademico(anio);
        return s;
    }

    private Matricula crearMatricula(EstadoMatricula estado, boolean anioVigente) {
        Matricula m = new Matricula();
        m.setId(1L);
        m.setEstado(estado);
        m.setEstudiante(crearEstudiante(crearApoderado()));
        m.setSeccion(crearSeccion(anioVigente));
        return m;
    }

    private PeriodoMatricula crearPeriodo(boolean vigente) {
        PeriodoMatricula p = new PeriodoMatricula();
        p.setFechaInicio(vigente ? LocalDate.now().minusDays(10) : LocalDate.now().minusDays(60));
        p.setFechaFin(vigente ? LocalDate.now().plusDays(20) : LocalDate.now().minusDays(1));
        return p;
    }

    private void autenticar(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    private void stubGuardadoMatricula(Long id) {
        when(compromisoPagoRepository.existsByMatriculaIdAndTipoCompromiso(1L, TipoCompromiso.MATRICULA))
                .thenReturn(false);
        when(compromisoPagoRepository.save(any(CompromisoPago.class))).thenAnswer(inv -> {
            CompromisoPago c = inv.getArgument(0);
            c.setId(id);
            return c;
        });
    }

    // --- matrícula ---

    @Test
    void emitirCompromisoMatricula_porMatriculaId_exitoso() {
        autenticar(crearAdmin());
        Matricula matricula = crearMatricula(EstadoMatricula.PENDIENTE, true);

        when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodo(true)));
        stubGuardadoMatricula(20L);

        CompromisoPagoResponse r = service.emitirCompromisoMatricula(
                new EmitirCompromisoMatriculaRequest(1L, null, null));

        assertEquals(20L, r.id());
        assertEquals(TipoCompromiso.MATRICULA, r.tipoCompromiso());
        assertEquals(EstadoCompromisoPago.PENDIENTE, r.estado());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void emitirCompromisoMatricula_porEstudianteYAnio_exitoso() {
        autenticar(crearAdmin());
        Matricula matricula = crearMatricula(EstadoMatricula.PENDIENTE, true);

        when(estudianteRepository.existsById(5L)).thenReturn(true);
        when(matriculaRepository.buscarPorEstudianteYAnioId(5L, 1L, EstadoMatricula.PENDIENTE))
                .thenReturn(List.of(matricula));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodo(true)));
        stubGuardadoMatricula(20L);

        CompromisoPagoResponse r = service.emitirCompromisoMatricula(
                new EmitirCompromisoMatriculaRequest(null, 5L, 1L));

        assertEquals(20L, r.id());
        assertEquals(1L, r.matriculaId());
        assertEquals(EstadoCompromisoPago.PENDIENTE, r.estado());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void emitirCompromisoMatricula_sinIdentificadores_rechaza() {
        autenticar(crearAdmin());

        assertThrows(ReglaNegocioException.class,
                () -> service.emitirCompromisoMatricula(
                        new EmitirCompromisoMatriculaRequest(null, null, null)));

        verify(compromisoPagoRepository, never()).save(any(CompromisoPago.class));
    }

    @Test
    void emitirCompromisoMatricula_estudianteNoExiste_rechaza() {
        autenticar(crearAdmin());
        when(estudianteRepository.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.emitirCompromisoMatricula(
                        new EmitirCompromisoMatriculaRequest(null, 99L, 1L)));

        verify(compromisoPagoRepository, never()).save(any(CompromisoPago.class));
    }

    @Test
    void emitirCompromisoMatricula_sinMatriculaPendiente_rechaza() {
        autenticar(crearAdmin());
        when(estudianteRepository.existsById(5L)).thenReturn(true);
        when(matriculaRepository.buscarPorEstudianteYAnioId(5L, 1L, EstadoMatricula.PENDIENTE))
                .thenReturn(List.of());

        assertThrows(ReglaNegocioException.class,
                () -> service.emitirCompromisoMatricula(
                        new EmitirCompromisoMatriculaRequest(null, 5L, 1L)));

        verify(compromisoPagoRepository, never()).save(any(CompromisoPago.class));
    }

    @Test
    void emitirCompromisoMatricula_usaMontoConfigurado() {
        autenticar(crearAdmin());
        Matricula matricula = crearMatricula(EstadoMatricula.PENDIENTE, true);
        ReflectionTestUtils.setField(service, "montoMatriculaPredeterminado", new BigDecimal("999.00"));

        when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodo(true)));
        stubGuardadoMatricula(20L);

        CompromisoPagoResponse r = service.emitirCompromisoMatricula(
                new EmitirCompromisoMatriculaRequest(1L, null, null));

        assertEquals(new BigDecimal("999.00"), r.montoOriginal());
        assertEquals(new BigDecimal("999.00"), r.montoTotal());
    }

    @Test
    void emitirCompromisoMatricula_rolNoAdmin_rechaza() {
        autenticar(crearApoderado());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.emitirCompromisoMatricula(
                        new EmitirCompromisoMatriculaRequest(1L, null, null)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // --- pensión ---

    @Test
    void emitirCompromisoPension_porMatriculaId_exitoso() {
        autenticar(crearAdmin());
        Matricula matricula = crearMatricula(EstadoMatricula.CONFIRMADA, true);

        int indice = Math.max(0, Math.min(LocalDate.now().getMonthValue() - 3, Mes.values().length - 1));
        Mes mesActual = Mes.values()[indice];

        when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
        when(compromisoPagoRepository.existsByMatriculaIdAndTipoCompromisoAndMes(1L, TipoCompromiso.PENSION, mesActual))
                .thenReturn(false);
        when(compromisoPagoRepository.save(any(CompromisoPago.class))).thenAnswer(inv -> {
            CompromisoPago c = inv.getArgument(0);
            c.setId(21L);
            return c;
        });

        CompromisoPagoResponse r = service.emitirCompromisoPension(
                new EmitirCompromisoPensionRequest(1L, null, null, mesActual));

        assertEquals(21L, r.id());
        assertEquals(TipoCompromiso.PENSION, r.tipoCompromiso());
        assertEquals(mesActual, r.mes());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void emitirCompromisoPension_porEstudianteYAnio_exitoso() {
        autenticar(crearAdmin());
        Matricula matricula = crearMatricula(EstadoMatricula.CONFIRMADA, true);

        int indice = Math.max(0, Math.min(LocalDate.now().getMonthValue() - 3, Mes.values().length - 1));
        Mes mesActual = Mes.values()[indice];

        when(estudianteRepository.existsById(5L)).thenReturn(true);
        when(matriculaRepository.buscarPorEstudianteYAnioId(5L, 1L, EstadoMatricula.CONFIRMADA))
                .thenReturn(List.of(matricula));
        when(compromisoPagoRepository.existsByMatriculaIdAndTipoCompromisoAndMes(1L, TipoCompromiso.PENSION, mesActual))
                .thenReturn(false);
        when(compromisoPagoRepository.save(any(CompromisoPago.class))).thenAnswer(inv -> {
            CompromisoPago c = inv.getArgument(0);
            c.setId(21L);
            return c;
        });

        CompromisoPagoResponse r = service.emitirCompromisoPension(
                new EmitirCompromisoPensionRequest(null, 5L, 1L, mesActual));

        assertEquals(21L, r.id());
        assertEquals(1L, r.matriculaId());
        assertEquals(mesActual, r.mes());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void emitirCompromisoPension_matriculaNoConfirmada_rechaza() {
        autenticar(crearAdmin());
        Matricula matricula = crearMatricula(EstadoMatricula.PENDIENTE, true);
        when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));

        assertThrows(ReglaNegocioException.class,
                () -> service.emitirCompromisoPension(
                        new EmitirCompromisoPensionRequest(1L, null, null, Mes.MARZO)));

        verify(compromisoPagoRepository, never()).save(any(CompromisoPago.class));
    }

    @Test
    void emitirCompromisoPension_sinMatriculaConfirmada_rechaza() {
        autenticar(crearAdmin());
        when(estudianteRepository.existsById(5L)).thenReturn(true);
        when(matriculaRepository.buscarPorEstudianteYAnioId(5L, 1L, EstadoMatricula.CONFIRMADA))
                .thenReturn(List.of());

        assertThrows(ReglaNegocioException.class,
                () -> service.emitirCompromisoPension(
                        new EmitirCompromisoPensionRequest(null, 5L, 1L, Mes.MARZO)));

        verify(compromisoPagoRepository, never()).save(any(CompromisoPago.class));
    }

    @Test
    void emitirCompromisoPension_mesFuturo_rechaza() {
        Assumptions.assumeTrue(LocalDate.now().getMonthValue() < 12,
                "En diciembre no existe un mes futuro dentro del año académico");

        autenticar(crearAdmin());
        Matricula matricula = crearMatricula(EstadoMatricula.CONFIRMADA, true);
        when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));

        assertThrows(ReglaNegocioException.class,
                () -> service.emitirCompromisoPension(
                        new EmitirCompromisoPensionRequest(1L, null, null, Mes.DICIEMBRE)));

        verify(compromisoPagoRepository, never()).save(any(CompromisoPago.class));
    }

    // --- emisión masiva de matrícula ---

    @Test
    void emitirCompromisosMatriculaLote_exitoso() {
        autenticar(crearAdmin());

        Matricula primera = crearMatricula(EstadoMatricula.PENDIENTE, true);
        Matricula segunda = crearMatricula(EstadoMatricula.PENDIENTE, true);
        segunda.setId(2L);

        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(true)));
        when(matriculaRepository.buscarPendientesPorAnioConDetalle(1L, EstadoMatricula.PENDIENTE))
                .thenReturn(List.of(primera, segunda));
        when(compromisoPagoRepository.existsByMatriculaIdAndTipoCompromiso(1L, TipoCompromiso.MATRICULA))
                .thenReturn(false);
        when(compromisoPagoRepository.existsByMatriculaIdAndTipoCompromiso(2L, TipoCompromiso.MATRICULA))
                .thenReturn(true);
        when(compromisoPagoRepository.save(any(CompromisoPago.class))).thenAnswer(inv -> {
            CompromisoPago c = inv.getArgument(0);
            c.setId(20L);
            return c;
        });

        EmitirCompromisosMatriculaLoteResponse r = service.emitirCompromisosMatriculaLote(
                new EmitirCompromisosMatriculaLoteRequest(1L));

        assertEquals(1, r.emitidos());
        assertEquals(1, r.omitidos());
        assertEquals(0, r.errores().size());
        verify(servicioNotificacionEmail).enviarCorreoCompromisoPago(any(), any(), any(), any(), any());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void emitirCompromisosMatriculaLote_continuaSiUnaFalla() {
        autenticar(crearAdmin());

        Matricula primera = crearMatricula(EstadoMatricula.PENDIENTE, true);
        Matricula segunda = crearMatricula(EstadoMatricula.PENDIENTE, true);
        segunda.setId(2L);

        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(true)));
        when(matriculaRepository.buscarPendientesPorAnioConDetalle(1L, EstadoMatricula.PENDIENTE))
                .thenReturn(List.of(primera, segunda));
        when(compromisoPagoRepository.existsByMatriculaIdAndTipoCompromiso(any(), any()))
                .thenReturn(false);
        when(compromisoPagoRepository.save(any(CompromisoPago.class))).thenAnswer(inv -> {
            CompromisoPago c = inv.getArgument(0);
            if (c.getMatricula().getId().equals(1L)) {
                throw new RuntimeException("Fallo simulado");
            }
            c.setId(21L);
            return c;
        });

        EmitirCompromisosMatriculaLoteResponse r = service.emitirCompromisosMatriculaLote(
                new EmitirCompromisosMatriculaLoteRequest(1L));

        assertEquals(1, r.emitidos());
        assertEquals(0, r.omitidos());
        assertEquals(1, r.errores().size());
        assertEquals(1L, r.errores().get(0).matriculaId());
    }

    @Test
    void emitirCompromisosMatriculaLote_periodoNoVigente_rechaza() {
        autenticar(crearAdmin());
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(false)));

        assertThrows(ReglaNegocioException.class,
                () -> service.emitirCompromisosMatriculaLote(
                        new EmitirCompromisosMatriculaLoteRequest(1L)));

        verify(matriculaRepository, never()).buscarPendientesPorAnioConDetalle(any(), any());
    }

    @Test
    void emitirCompromisosMatriculaLote_rolNoAdmin_rechaza() {
        autenticar(crearApoderado());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.emitirCompromisosMatriculaLote(
                        new EmitirCompromisosMatriculaLoteRequest(1L)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(periodoMatriculaRepository, never()).findByAnioAcademicoId(any());
    }

    @Test
    void emitirCompromisosPensionLote_exitoso() {
        Assumptions.assumeTrue(LocalDate.now().getMonthValue() >= 3,
                "No hay meses no futuros antes de marzo");

        autenticar(crearAdmin());

        AnioAcademico anio = crearSeccion(true).getAnioAcademico();
        Mes mesActual = Mes.values()[Math.min(
                LocalDate.now().getMonthValue() - 3, Mes.values().length - 1)];

        Matricula primera = crearMatricula(EstadoMatricula.CONFIRMADA, true);
        Matricula segunda = crearMatricula(EstadoMatricula.CONFIRMADA, true);
        segunda.setId(2L);

        when(anioAcademicoRepository.findById(1L)).thenReturn(Optional.of(anio));
        when(matriculaRepository.buscarPorAnioYEstadoConDetalle(
                1L, EstadoMatricula.CONFIRMADA, null, null))
                .thenReturn(List.of(primera, segunda));
        when(compromisoPagoRepository.existsByMatriculaIdAndTipoCompromisoAndMes(
                1L, TipoCompromiso.PENSION, mesActual)).thenReturn(false);
        when(compromisoPagoRepository.existsByMatriculaIdAndTipoCompromisoAndMes(
                2L, TipoCompromiso.PENSION, mesActual)).thenReturn(true);
        when(compromisoPagoRepository.save(any(CompromisoPago.class))).thenAnswer(inv -> {
            CompromisoPago c = inv.getArgument(0);
            c.setId(30L);
            return c;
        });

        EmitirCompromisosPensionLoteResponse r = service.emitirCompromisosPensionLote(
                new EmitirCompromisosPensionLoteRequest(1L, mesActual, null, null));

        assertEquals(1, r.emitidos());
        assertEquals(1, r.omitidos());
        assertEquals(0, r.errores().size());
        verify(servicioNotificacionEmail).enviarCorreoCompromisoPago(any(), any(), any(), any(), any());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void emitirCompromisosPensionLote_continuaSiUnaFalla() {
        Assumptions.assumeTrue(LocalDate.now().getMonthValue() >= 3,
                "No hay meses no futuros antes de marzo");

        autenticar(crearAdmin());
        AnioAcademico anio = crearSeccion(true).getAnioAcademico();
        Mes mesActual = Mes.values()[Math.min(
                LocalDate.now().getMonthValue() - 3, Mes.values().length - 1)];

        Matricula primera = crearMatricula(EstadoMatricula.CONFIRMADA, true);
        Matricula segunda = crearMatricula(EstadoMatricula.CONFIRMADA, true);
        segunda.setId(2L);

        when(anioAcademicoRepository.findById(1L)).thenReturn(Optional.of(anio));
        when(matriculaRepository.buscarPorAnioYEstadoConDetalle(
                1L, EstadoMatricula.CONFIRMADA, null, null))
                .thenReturn(List.of(primera, segunda));
        when(compromisoPagoRepository.existsByMatriculaIdAndTipoCompromisoAndMes(
                any(), any(), any())).thenReturn(false);
        when(compromisoPagoRepository.save(any(CompromisoPago.class))).thenAnswer(inv -> {
            CompromisoPago c = inv.getArgument(0);
            if (c.getMatricula().getId().equals(1L)) {
                throw new RuntimeException("Fallo simulado");
            }
            c.setId(31L);
            return c;
        });

        EmitirCompromisosPensionLoteResponse r = service.emitirCompromisosPensionLote(
                new EmitirCompromisosPensionLoteRequest(1L, mesActual, null, null));

        assertEquals(1, r.emitidos());
        assertEquals(0, r.omitidos());
        assertEquals(1, r.errores().size());
        assertEquals(1L, r.errores().get(0).matriculaId());
    }

    @Test
    void emitirCompromisosPensionLote_anioNoVigente_rechaza() {
        autenticar(crearAdmin());
        AnioAcademico anio = crearSeccion(true).getAnioAcademico();
        anio.setFechaInicio(LocalDate.now().minusYears(2));
        anio.setFechaFin(LocalDate.now().minusYears(1));
        when(anioAcademicoRepository.findById(1L)).thenReturn(Optional.of(anio));

        assertThrows(ReglaNegocioException.class,
                () -> service.emitirCompromisosPensionLote(
                        new EmitirCompromisosPensionLoteRequest(1L, Mes.MARZO, null, null)));

        verify(matriculaRepository, never()).buscarPorAnioYEstadoConDetalle(any(), any(), any(), any());
    }

    @Test
    void emitirCompromisosPensionLote_mesFuturo_rechaza() {
        Assumptions.assumeTrue(LocalDate.now().getMonthValue() < 12,
                "En diciembre no hay un mes futuro");

        autenticar(crearAdmin());
        AnioAcademico anio = crearSeccion(true).getAnioAcademico();
        when(anioAcademicoRepository.findById(1L)).thenReturn(Optional.of(anio));

        assertThrows(ReglaNegocioException.class,
                () -> service.emitirCompromisosPensionLote(
                        new EmitirCompromisosPensionLoteRequest(1L, Mes.DICIEMBRE, null, null)));

        verify(matriculaRepository, never()).buscarPorAnioYEstadoConDetalle(any(), any(), any(), any());
    }

    @Test
    void emitirCompromisosPensionLote_rolNoAdmin_rechaza() {
        autenticar(crearApoderado());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.emitirCompromisosPensionLote(
                        new EmitirCompromisosPensionLoteRequest(1L, Mes.MARZO, null, null)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(anioAcademicoRepository, never()).findById(any());
    }

    @Test
    void emitirCompromisosPensionLote_sinMatriculas_devuelveCeros() {
        autenticar(crearAdmin());
        AnioAcademico anio = crearSeccion(true).getAnioAcademico();
        when(anioAcademicoRepository.findById(1L)).thenReturn(Optional.of(anio));
        when(matriculaRepository.buscarPorAnioYEstadoConDetalle(
                1L, EstadoMatricula.CONFIRMADA, 1L, 2L))
                .thenReturn(List.of());

        EmitirCompromisosPensionLoteResponse r = service.emitirCompromisosPensionLote(
                new EmitirCompromisosPensionLoteRequest(1L, Mes.MARZO, 1L, 2L));

        assertEquals(0, r.emitidos());
        assertEquals(0, r.omitidos());
        assertEquals(0, r.errores().size());
    }

    @Test
    void emitirCompromisosMatriculaLote_sinPendientes_devuelveCeros() {
        autenticar(crearAdmin());
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(true)));
        when(matriculaRepository.buscarPendientesPorAnioConDetalle(1L, EstadoMatricula.PENDIENTE))
                .thenReturn(List.of());

        EmitirCompromisosMatriculaLoteResponse r = service.emitirCompromisosMatriculaLote(
                new EmitirCompromisosMatriculaLoteRequest(1L));

        assertEquals(0, r.emitidos());
        assertEquals(0, r.omitidos());
        assertEquals(0, r.errores().size());
    }
}
