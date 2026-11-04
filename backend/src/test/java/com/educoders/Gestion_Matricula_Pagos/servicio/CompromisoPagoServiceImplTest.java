package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.CompromisoPagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoPagoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoPensionRequest;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.Mes;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompromisoPagoServiceImplTest {

    @Mock private CompromisoPagoRepository compromisoPagoRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private PeriodoMatriculaRepository periodoMatriculaRepository;
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

    // --- matrícula ---

    @Test
    void emitirCompromisoMatricula_exitoso() {
        autenticar(crearAdmin());
        Matricula matricula = crearMatricula(EstadoMatricula.PENDIENTE, true);

        when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodo(true)));
        when(compromisoPagoRepository.existsByMatriculaIdAndTipoCompromiso(1L, TipoCompromiso.MATRICULA))
                .thenReturn(false);
        when(compromisoPagoRepository.save(any(CompromisoPago.class))).thenAnswer(inv -> {
            CompromisoPago c = inv.getArgument(0);
            c.setId(20L);
            return c;
        });

        CompromisoPagoResponse r = service.emitirCompromisoMatricula(
                new EmitirCompromisoMatriculaRequest(1L, new BigDecimal("500.00")));

        assertEquals(20L, r.id());
        assertEquals(TipoCompromiso.MATRICULA, r.tipoCompromiso());
        assertEquals(EstadoCompromisoPago.PENDIENTE, r.estado());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void emitirCompromisoMatricula_rolNoAdmin_rechaza() {
        autenticar(crearApoderado());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.emitirCompromisoMatricula(
                        new EmitirCompromisoMatriculaRequest(1L, null)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // --- pensión ---

    @Test
    void emitirCompromisoPension_matriculaNoConfirmada_rechaza() {
        autenticar(crearAdmin());
        Matricula matricula = crearMatricula(EstadoMatricula.PENDIENTE, true);
        when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));

        assertThrows(ReglaNegocioException.class,
                () -> service.emitirCompromisoPension(
                        new EmitirCompromisoPensionRequest(1L, Mes.MARZO, null)));

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
                        new EmitirCompromisoPensionRequest(1L, Mes.DICIEMBRE, null)));

        verify(compromisoPagoRepository, never()).save(any(CompromisoPago.class));
    }

    @Test
    void emitirCompromisoPension_exitoso() {
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
                new EmitirCompromisoPensionRequest(1L, mesActual, new BigDecimal("450.00")));

        assertEquals(21L, r.id());
        assertEquals(TipoCompromiso.PENSION, r.tipoCompromiso());
        assertEquals(mesActual, r.mes());
        verify(auditoriaService).registrarAuditoria(any());
    }
}