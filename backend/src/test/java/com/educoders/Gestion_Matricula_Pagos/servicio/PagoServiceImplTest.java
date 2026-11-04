package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.Pago;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.CompromisoPagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.PagoApoderadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RechazarPagoRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarPagoRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ValidarPagoRequest;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoPago;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.servicio.ServicioAlmacenamientoArchivo;
import com.educoders.Infraestructura_Servicios.servicio.ServicioNotificacionEmail;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock private PagoRepository pagoRepository;
    @Mock private CompromisoPagoRepository compromisoPagoRepository;
    @Mock private PeriodoMatriculaRepository periodoMatriculaRepository;
    @Mock private ServicioAlmacenamientoArchivo servicioAlmacenamientoArchivo;
    @Mock private ServicioNotificacionEmail servicioNotificacionEmail;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private PagoServiceImpl service;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // --- datos ---

    private Apoderado crearApoderado(Long id) {
        Apoderado a = new Apoderado();
        a.setId(id);
        a.setNombre("Carlos");
        a.setApellidoPaterno("Ramirez");
        a.setApellidoMaterno("Torres");
        a.setEmail("carlos@gmail.com");
        return a;
    }

    private Administrador crearAdmin(Long id) {
        Administrador a = new Administrador();
        a.setId(id);
        a.setNombre("Admin");
        a.setApellidoPaterno("Sistema");
        return a;
    }

    private Estudiante crearEstudiante(Long id, Apoderado apoderado) {
        Estudiante e = new Estudiante();
        e.setId(id);
        e.setNombre("Luis");
        e.setApellidoPaterno("Ramirez");
        e.setApellidoMaterno("Torres");
        e.setApoderado(apoderado);
        return e;
    }

    private Seccion crearSeccion(Long id, AnioAcademico anio) {
        Grado g = new Grado();
        g.setId(1L);
        g.setNombre("1er Grado");
        Seccion s = new Seccion();
        s.setId(id);
        s.setNombre("A");
        s.setGrado(g);
        s.setAnioAcademico(anio);
        return s;
    }

    private AnioAcademico crearAnio(Long id) {
        AnioAcademico a = new AnioAcademico();
        a.setId(id);
        a.setAnio(2027);
        a.setFechaInicio(LocalDate.now().minusMonths(3));
        a.setFechaFin(LocalDate.now().plusMonths(9));
        return a;
    }

    private Matricula crearMatricula(Long id, EstadoMatricula estado, Estudiante est, Seccion sec) {
        Matricula m = new Matricula();
        m.setId(id);
        m.setEstado(estado);
        m.setEstudiante(est);
        m.setSeccion(sec);
        return m;
    }

    private CompromisoPago crearCompromiso(Long id, TipoCompromiso tipo, EstadoCompromisoPago estado, Matricula m) {
        CompromisoPago c = new CompromisoPago();
        c.setId(id);
        c.setTipoCompromiso(tipo);
        c.setEstado(estado);
        c.setMatricula(m);
        c.setMontoOriginal(new BigDecimal("500.00"));
        c.setMontoMora(BigDecimal.ZERO);
        return c;
    }

    private Pago crearPago(Long id, EstadoPago estado, CompromisoPago c, Apoderado a) {
        Pago p = new Pago();
        p.setId(id);
        p.setEstado(estado);
        p.setCompromisoPago(c);
        p.setApoderadoPagador(a);
        p.setMontoPagado(new BigDecimal("500.00"));
        p.setFechaRegistro(LocalDate.now());
        return p;
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

    // --- registrar pago ---

    @Test
    void registrarPagoApoderado_matriculaFueraDePeriodo_rechaza() {
        Apoderado apoderado = crearApoderado(4L);
        autenticar(apoderado);

        AnioAcademico anio = crearAnio(1L);
        Seccion seccion = crearSeccion(1L, anio);
        Matricula matricula = crearMatricula(1L, EstadoMatricula.PENDIENTE,
                crearEstudiante(5L, apoderado), seccion);
        CompromisoPago compromiso = crearCompromiso(1L, TipoCompromiso.MATRICULA,
                EstadoCompromisoPago.PENDIENTE, matricula);

        when(compromisoPagoRepository.findById(1L)).thenReturn(Optional.of(compromiso));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(false)));

        assertThrows(ReglaNegocioException.class, () -> service.registrarPagoApoderado(
                new RegistrarPagoRequest(1L, new BigDecimal("500.00"), null)));

        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void registrarPagoApoderado_exitoso() {
        Apoderado apoderado = crearApoderado(4L);
        autenticar(apoderado);

        AnioAcademico anio = crearAnio(1L);
        Seccion seccion = crearSeccion(1L, anio);
        Matricula matricula = crearMatricula(1L, EstadoMatricula.PENDIENTE,
                crearEstudiante(5L, apoderado), seccion);
        CompromisoPago compromiso = crearCompromiso(1L, TipoCompromiso.MATRICULA,
                EstadoCompromisoPago.PENDIENTE, matricula);

        when(compromisoPagoRepository.findById(1L)).thenReturn(Optional.of(compromiso));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(true)));
        when(pagoRepository.existsByCompromisoPagoIdAndEstado(1L, EstadoPago.PENDIENTE)).thenReturn(false);
        when(servicioAlmacenamientoArchivo.guardarArchivo(any(), anyString())).thenReturn("https://cloudinary/x.pdf");
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        MockMultipartFile pdf = new MockMultipartFile("comprobante", "boleta.pdf",
                "application/pdf", "pdf".getBytes());

        PagoApoderadoResponse r = service.registrarPagoApoderado(
                new RegistrarPagoRequest(1L, new BigDecimal("500.00"), pdf));

        assertEquals(10L, r.id());
        assertEquals(EstadoPago.PENDIENTE, r.estado());
        assertEquals("https://cloudinary/x.pdf", r.evidenciaUrl());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void registrarPagoApoderado_noEsSuEstudiante_rechaza() {
        Apoderado apoderado = crearApoderado(4L);
        autenticar(apoderado);

        AnioAcademico anio = crearAnio(1L);
        Seccion seccion = crearSeccion(1L, anio);
        // El estudiante pertenece a otro apoderado (99)
        Matricula matricula = crearMatricula(1L, EstadoMatricula.PENDIENTE,
                crearEstudiante(5L, crearApoderado(99L)), seccion);
        CompromisoPago compromiso = crearCompromiso(1L, TipoCompromiso.MATRICULA,
                EstadoCompromisoPago.PENDIENTE, matricula);

        when(compromisoPagoRepository.findById(1L)).thenReturn(Optional.of(compromiso));

        assertThrows(ReglaNegocioException.class, () -> service.registrarPagoApoderado(
                new RegistrarPagoRequest(1L, new BigDecimal("500.00"), null)));

        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void registrarPago_rolNoApoderado_rechaza() {
        autenticar(crearAdmin(1L));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.registrarPagoApoderado(
                        new RegistrarPagoRequest(1L, new BigDecimal("500.00"), null)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // --- validar / rechazar ---

    @Test
    void validarPagoAdmin_fueraDePeriodoMatricula_rechaza() {
        autenticar(crearAdmin(1L));

        Apoderado apoderado = crearApoderado(4L);
        AnioAcademico anio = crearAnio(1L);
        Seccion seccion = crearSeccion(1L, anio);
        Matricula matricula = crearMatricula(1L, EstadoMatricula.PENDIENTE,
                crearEstudiante(5L, apoderado), seccion);
        CompromisoPago compromiso = crearCompromiso(1L, TipoCompromiso.MATRICULA,
                EstadoCompromisoPago.PENDIENTE, matricula);
        Pago pago = crearPago(10L, EstadoPago.PENDIENTE, compromiso, apoderado);

        when(pagoRepository.findByIdConDetalle(10L)).thenReturn(Optional.of(pago));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(false)));

        assertThrows(ReglaNegocioException.class,
                () -> service.validarPagoAdmin(10L, new ValidarPagoRequest()));

        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void validarPagoAdmin_exitoso() {
        autenticar(crearAdmin(1L));

        Apoderado apoderado = crearApoderado(4L);
        AnioAcademico anio = crearAnio(1L);
        Seccion seccion = crearSeccion(1L, anio);
        Matricula matricula = crearMatricula(1L, EstadoMatricula.PENDIENTE,
                crearEstudiante(5L, apoderado), seccion);
        CompromisoPago compromiso = crearCompromiso(1L, TipoCompromiso.MATRICULA,
                EstadoCompromisoPago.PENDIENTE, matricula);
        Pago pago = crearPago(10L, EstadoPago.PENDIENTE, compromiso, apoderado);

        when(pagoRepository.findByIdConDetalle(10L)).thenReturn(Optional.of(pago));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(true)));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        service.validarPagoAdmin(10L, new ValidarPagoRequest());

        assertEquals(EstadoPago.VALIDADO, pago.getEstado());
        assertEquals(EstadoCompromisoPago.PAGADO, compromiso.getEstado());
        assertEquals(EstadoMatricula.CONFIRMADA, matricula.getEstado());
        verify(servicioNotificacionEmail).enviarCorreoValidacionPago(any(), any(), any(), any(), any());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void rechazarPagoAdmin_exitoso() {
        autenticar(crearAdmin(1L));

        Apoderado apoderado = crearApoderado(4L);
        AnioAcademico anio = crearAnio(1L);
        Seccion seccion = crearSeccion(1L, anio);
        Matricula matricula = crearMatricula(1L, EstadoMatricula.PENDIENTE,
                crearEstudiante(5L, apoderado), seccion);
        CompromisoPago compromiso = crearCompromiso(1L, TipoCompromiso.MATRICULA,
                EstadoCompromisoPago.PENDIENTE, matricula);
        Pago pago = crearPago(10L, EstadoPago.PENDIENTE, compromiso, apoderado);

        when(pagoRepository.findByIdConDetalle(10L)).thenReturn(Optional.of(pago));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(true)));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        service.rechazarPagoAdmin(10L, new RechazarPagoRequest("Comprobante ilegible"));

        assertEquals(EstadoPago.RECHAZADO, pago.getEstado());
        assertEquals("Comprobante ilegible", pago.getMotivo());
        assertEquals(EstadoCompromisoPago.PENDIENTE, compromiso.getEstado());
        assertEquals(EstadoMatricula.PENDIENTE, matricula.getEstado());
        verify(servicioNotificacionEmail).enviarCorreoRechazoPago(any(), any(), any(), any(), anyString());
        verify(auditoriaService).registrarAuditoria(any());
    }
}