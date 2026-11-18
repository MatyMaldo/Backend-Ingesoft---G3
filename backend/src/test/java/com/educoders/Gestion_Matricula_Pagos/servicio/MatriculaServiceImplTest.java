package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.Pago;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.CompromisoPagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ActualizarSeccionMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaAdminListResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculasPorEstadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.MatriculaResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.RegistrarMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.SeccionEstudianteResponse;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceImplTest {

    @Mock private MatriculaRepository matriculaRepository;
    @Mock private PeriodoMatriculaRepository periodoMatriculaRepository;
    @Mock private SeccionRepository seccionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private CompromisoPagoRepository compromisoPagoRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private MatriculaServiceImpl service;

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
        return a;
    }

    private Estudiante crearEstudiante() {
        Estudiante e = new Estudiante();
        e.setId(5L);
        e.setNombre("Luis");
        e.setApellidoPaterno("Ramirez");
        e.setApellidoMaterno("Torres");
        e.setCodigo("EST-001");
        e.setActivo(true);
        return e;
    }

    private Seccion crearSeccion() {
        AnioAcademico anio = new AnioAcademico();
        anio.setId(1L);
        anio.setAnio(2027);
        Grado g = new Grado();
        g.setId(1L);
        g.setNombre("1er Grado");
        Seccion s = new Seccion();
        s.setId(1L);
        s.setNombre("A");
        s.setCapacidadMaxima(30);
        s.setGrado(g);
        s.setAnioAcademico(anio);
        return s;
    }

    private Seccion crearSeccion(Long id, String nombre, AnioAcademico anio) {
        Grado g = new Grado();
        g.setId(1L);
        g.setNombre("1er Grado");
        Seccion s = new Seccion();
        s.setId(id);
        s.setNombre(nombre);
        s.setCapacidadMaxima(30);
        s.setGrado(g);
        s.setAnioAcademico(anio);
        return s;
    }

    private PeriodoMatricula crearPeriodo(Seccion seccion, boolean vigente) {
        PeriodoMatricula p = crearPeriodo(vigente);
        p.setAnioAcademico(seccion.getAnioAcademico());
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

    @Test
    void registrarMatricula_exitoso() {
        autenticar(crearAdmin());
        Seccion seccion = crearSeccion();
        Estudiante estudiante = crearEstudiante();

        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodo(true)));
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoIn(anyLong(), anyLong(), anyCollection()))
                .thenReturn(false);
        when(matriculaRepository.countBySeccionIdAndEstadoIn(anyLong(), anyCollection())).thenReturn(0L);
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(inv -> {
            Matricula m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        MatriculaResponse r = service.registrarMatricula(new RegistrarMatriculaRequest(5L, 1L));

        assertEquals(100L, r.id());
        assertEquals(EstadoMatricula.PENDIENTE, r.estado());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void registrarMatricula_fueraDePeriodo_rechaza() {
        autenticar(crearAdmin());
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(crearSeccion()));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L)).thenReturn(Optional.of(crearPeriodo(false)));

        assertThrows(ReglaNegocioException.class,
                () -> service.registrarMatricula(new RegistrarMatriculaRequest(5L, 1L)));

        verify(matriculaRepository, never()).save(any(Matricula.class));
    }

    @Test
    void obtenerSeccionEstudiante_exitoso() {
        autenticar(crearAdmin());
        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setEstado(EstadoMatricula.CONFIRMADA);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(crearSeccion());

        when(matriculaRepository.buscarPorEstudianteYAnioIdYEstados(
                5L, 1L, List.of(EstadoMatricula.PENDIENTE, EstadoMatricula.CONFIRMADA)))
                .thenReturn(List.of(matricula));

        SeccionEstudianteResponse r = service.obtenerSeccionEstudiante(5L, 1L);

        assertEquals(100L, r.matriculaId());
        assertEquals(5L, r.estudianteId());
        assertEquals(1L, r.anioAcademicoId());
        assertEquals(2027, r.anio());
        assertEquals(EstadoMatricula.CONFIRMADA, r.estado());
        assertEquals(1L, r.gradoId());
        assertEquals("1er Grado", r.gradoNombre());
        assertEquals(1L, r.seccionId());
        assertEquals("A", r.seccionNombre());
    }

    @Test
    void obtenerSeccionEstudiante_sinMatricula_rechaza() {
        autenticar(crearAdmin());
        when(matriculaRepository.buscarPorEstudianteYAnioIdYEstados(
                5L, 1L, List.of(EstadoMatricula.PENDIENTE, EstadoMatricula.CONFIRMADA)))
                .thenReturn(List.of());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtenerSeccionEstudiante(5L, 1L));
    }

    @Test
    void listarMatriculas_admin_exitoso() {
        autenticar(crearAdmin());

        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setFechaAsignacion(LocalDate.now());
        matricula.setEstado(EstadoMatricula.CONFIRMADA);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(crearSeccion());

        Pageable pageable = PageRequest.of(0, 10);
        when(matriculaRepository.listarMatriculas(
                1L, 1L, 1L, EstadoMatricula.CONFIRMADA, 5L, "%luis%", pageable))
                .thenReturn(new PageImpl<>(List.of(matricula), pageable, 1));

        Page<MatriculaAdminListResponse> resultado = service.listarMatriculas(
                1L, 1L, 1L, EstadoMatricula.CONFIRMADA, 5L, "  Luis ", pageable);

        assertEquals(1, resultado.getTotalElements());
        MatriculaAdminListResponse fila = resultado.getContent().get(0);
        assertEquals(100L, fila.id());
        assertEquals(5L, fila.estudianteId());
        assertEquals("Luis Ramirez Torres", fila.nombreCompletoEstudiante());
        assertEquals(1L, fila.gradoId());
        assertEquals("1er Grado", fila.gradoNombre());
        assertEquals(1L, fila.seccionId());
        assertEquals("A", fila.seccionNombre());
        assertEquals(1L, fila.anioAcademicoId());
        assertEquals(2027, fila.anioAcademico());
    }

    @Test
    void listarMatriculas_rolNoAdmin_rechaza() {
        autenticar(crearApoderado());

        Pageable pageable = PageRequest.of(0, 10);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.listarMatriculas(null, null, null, null, null, null, pageable));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(matriculaRepository, never()).listarMatriculas(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void contarMatriculasPorEstado_sumaPorEstadoYCompletaLosQueFaltan() {
        autenticar(crearAdmin());

        when(matriculaRepository.contarPorEstado(1L, null, null, "%ana%"))
                .thenReturn(List.of(
                        new Object[]{EstadoMatricula.PENDIENTE, 3L},
                        new Object[]{EstadoMatricula.CANCELADA, 1L}));

        MatriculasPorEstadoResponse conteo = service.contarMatriculasPorEstado(1L, null, null, " Ana ");

        assertEquals(4L, conteo.total());
        assertEquals(3L, conteo.pendientes());
        assertEquals(0L, conteo.confirmadas());
        assertEquals(1L, conteo.canceladas());
    }

    @Test
    void contarMatriculasPorEstado_rolNoAdmin_rechaza() {
        autenticar(crearApoderado());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.contarMatriculasPorEstado(null, null, null, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(matriculaRepository, never()).contarPorEstado(any(), any(), any(), any());
    }

    @Test
    void listarMatriculas_busquedaEnBlanco_noFiltra() {
        autenticar(crearAdmin());

        Pageable pageable = PageRequest.of(0, 10);
        when(matriculaRepository.listarMatriculas(null, null, null, null, null, "", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<MatriculaAdminListResponse> resultado = service.listarMatriculas(
                null, null, null, null, null, "   ", pageable);

        assertEquals(0, resultado.getTotalElements());
        verify(matriculaRepository).listarMatriculas(null, null, null, null, null, "", pageable);
    }

    @Test
    void cancelarPendientesPorPeriodoVencido_cancelaSinCompromisoPagado() {
        Seccion seccion = crearSeccion();
        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(LocalDate.now().minusMonths(3));
        periodo.setFechaFin(LocalDate.now().minusDays(1));
        periodo.setAnioAcademico(seccion.getAnioAcademico());

        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setEstado(EstadoMatricula.PENDIENTE);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(seccion);

        when(periodoMatriculaRepository.findAll()).thenReturn(List.of(periodo));
        when(matriculaRepository.buscarSinCompromisoPagado(
                1L, EstadoMatricula.PENDIENTE, TipoCompromiso.MATRICULA, EstadoCompromisoPago.PAGADO))
                .thenReturn(List.of(matricula));

        int canceladas = service.cancelarPendientesPorPeriodoVencido();

        assertEquals(1, canceladas);
        assertEquals(EstadoMatricula.CANCELADA, matricula.getEstado());
        verify(matriculaRepository).saveAll(any());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void cancelarPendientesPorPeriodoVigente_noCancela() {
        Seccion seccion = crearSeccion();
        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setFechaInicio(LocalDate.now().minusMonths(1));
        periodo.setFechaFin(LocalDate.now().plusMonths(1));
        periodo.setAnioAcademico(seccion.getAnioAcademico());

        when(periodoMatriculaRepository.findAll()).thenReturn(List.of(periodo));

        int canceladas = service.cancelarPendientesPorPeriodoVencido();

        assertEquals(0, canceladas);
        verify(matriculaRepository, never()).saveAll(any());
        verify(auditoriaService, never()).registrarAuditoria(any());
    }

    @Test
    void registrarMatricula_rolNoAdmin_rechaza() {
        autenticar(crearApoderado());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.registrarMatricula(new RegistrarMatriculaRequest(5L, 1L)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void actualizarSeccion_exitoso_auditaCambio() {
        autenticar(crearAdmin());

        Seccion anterior = crearSeccion();
        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setFechaAsignacion(LocalDate.now());
        matricula.setEstado(EstadoMatricula.PENDIENTE);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(anterior);

        Seccion nueva = crearSeccion(2L, "B", anterior.getAnioAcademico());

        when(matriculaRepository.findByIdParaActualizar(100L))
                .thenReturn(Optional.of(matricula));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(true)));
        when(seccionRepository.findById(2L)).thenReturn(Optional.of(nueva));
        when(matriculaRepository
                .existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoInAndIdNot(
                        anyLong(), anyLong(), anyCollection(), anyLong()))
                .thenReturn(false);
        when(matriculaRepository.existsByEstudianteIdAndSeccionIdAndIdNot(
                anyLong(), anyLong(), anyLong())).thenReturn(false);
        when(matriculaRepository.countBySeccionIdAndEstadoInAndIdNot(
                anyLong(), anyCollection(), anyLong())).thenReturn(0L);
        when(matriculaRepository.save(any(Matricula.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(periodoMatriculaRepository.findAll())
                .thenReturn(List.of(crearPeriodo(anterior, true)));
        when(compromisoPagoRepository.findByMatriculaIdIn(any()))
                .thenReturn(List.of());

        MatriculaAdminListResponse respuesta = service.actualizarSeccion(
                100L, new ActualizarSeccionMatriculaRequest(2L));

        assertEquals(2L, respuesta.seccionId());
        assertEquals("B", respuesta.seccionNombre());
        assertTrue(respuesta.puedeEditar());
        assertTrue(respuesta.puedeEliminar());
        assertNull(respuesta.motivoNoEditar());
        assertNull(respuesta.motivoNoEliminar());
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void actualizarSeccion_noPendiente_rechaza() {
        autenticar(crearAdmin());

        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setEstado(EstadoMatricula.CONFIRMADA);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(crearSeccion());

        when(matriculaRepository.findByIdParaActualizar(100L))
                .thenReturn(Optional.of(matricula));

        assertThrows(ConflictoException.class,
                () -> service.actualizarSeccion(
                        100L, new ActualizarSeccionMatriculaRequest(2L)));

        verify(matriculaRepository, never()).save(any(Matricula.class));
    }

    @Test
    void actualizarSeccion_periodoVencido_rechaza() {
        autenticar(crearAdmin());

        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setEstado(EstadoMatricula.PENDIENTE);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(crearSeccion());

        when(matriculaRepository.findByIdParaActualizar(100L))
                .thenReturn(Optional.of(matricula));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(false)));

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizarSeccion(
                        100L, new ActualizarSeccionMatriculaRequest(2L)));

        verify(matriculaRepository, never()).save(any(Matricula.class));
    }

    @Test
    void actualizarSeccion_canceladaEnDestino_rechaza() {
        autenticar(crearAdmin());

        Seccion anterior = crearSeccion();
        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setEstado(EstadoMatricula.PENDIENTE);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(anterior);

        Seccion nueva = crearSeccion(2L, "B", anterior.getAnioAcademico());

        when(matriculaRepository.findByIdParaActualizar(100L))
                .thenReturn(Optional.of(matricula));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(true)));
        when(seccionRepository.findById(2L)).thenReturn(Optional.of(nueva));
        when(matriculaRepository
                .existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoInAndIdNot(
                        anyLong(), anyLong(), anyCollection(), anyLong()))
                .thenReturn(false);
        when(matriculaRepository.existsByEstudianteIdAndSeccionIdAndIdNot(
                anyLong(), anyLong(), anyLong())).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> service.actualizarSeccion(
                        100L, new ActualizarSeccionMatriculaRequest(2L)));

        verify(matriculaRepository, never()).save(any(Matricula.class));
    }

    @Test
    void eliminarMatricula_exitoso_borraCompromisoSinPagos() {
        autenticar(crearAdmin());

        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setEstado(EstadoMatricula.PENDIENTE);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(crearSeccion());

        CompromisoPago compromiso = new CompromisoPago();
        compromiso.setId(10L);
        compromiso.setTipoCompromiso(TipoCompromiso.MATRICULA);
        compromiso.setEstado(EstadoCompromisoPago.PENDIENTE);
        compromiso.setMatricula(matricula);

        when(matriculaRepository.findByIdParaActualizar(100L))
                .thenReturn(Optional.of(matricula));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(true)));
        when(compromisoPagoRepository.findByMatriculaId(100L))
                .thenReturn(List.of(compromiso));
        when(pagoRepository.findByCompromisoPagoId(10L)).thenReturn(List.of());

        service.eliminarMatricula(100L);

        verify(compromisoPagoRepository).delete(compromiso);
        verify(matriculaRepository).delete(matricula);
        verify(auditoriaService).registrarAuditoria(any());
    }

    @Test
    void eliminarMatricula_conPagos_rechaza() {
        autenticar(crearAdmin());

        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setEstado(EstadoMatricula.PENDIENTE);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(crearSeccion());

        CompromisoPago compromiso = new CompromisoPago();
        compromiso.setId(10L);
        compromiso.setTipoCompromiso(TipoCompromiso.MATRICULA);
        compromiso.setEstado(EstadoCompromisoPago.PENDIENTE);
        compromiso.setMatricula(matricula);

        when(matriculaRepository.findByIdParaActualizar(100L))
                .thenReturn(Optional.of(matricula));
        when(periodoMatriculaRepository.findByAnioAcademicoId(1L))
                .thenReturn(Optional.of(crearPeriodo(true)));
        when(compromisoPagoRepository.findByMatriculaId(100L))
                .thenReturn(List.of(compromiso));
        when(pagoRepository.findByCompromisoPagoId(10L))
                .thenReturn(List.of(new Pago()));

        assertThrows(ConflictoException.class, () -> service.eliminarMatricula(100L));

        verify(compromisoPagoRepository, never()).delete(any());
        verify(matriculaRepository, never()).delete(any(Matricula.class));
    }

    @Test
    void listarMatriculas_pendienteVigente_permiteEditarYEliminar() {
        autenticar(crearAdmin());

        Seccion seccion = crearSeccion();
        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setFechaAsignacion(LocalDate.now());
        matricula.setEstado(EstadoMatricula.PENDIENTE);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(seccion);

        Pageable pageable = PageRequest.of(0, 10);
        when(matriculaRepository.listarMatriculas(
                null, null, null, null, null, "", pageable))
                .thenReturn(new PageImpl<>(List.of(matricula), pageable, 1));
        when(periodoMatriculaRepository.findAll())
                .thenReturn(List.of(crearPeriodo(seccion, true)));
        when(compromisoPagoRepository.findByMatriculaIdIn(any()))
                .thenReturn(List.of());

        MatriculaAdminListResponse fila = service.listarMatriculas(
                null, null, null, null, null, null, pageable).getContent().get(0);

        assertTrue(fila.puedeEditar());
        assertTrue(fila.puedeEliminar());
        assertNull(fila.motivoNoEditar());
        assertNull(fila.motivoNoEliminar());
    }

    @Test
    void listarMatriculas_confirmada_noPermiteAcciones() {
        autenticar(crearAdmin());

        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setFechaAsignacion(LocalDate.now());
        matricula.setEstado(EstadoMatricula.CONFIRMADA);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(crearSeccion());

        Pageable pageable = PageRequest.of(0, 10);
        when(matriculaRepository.listarMatriculas(
                null, null, null, null, null, "", pageable))
                .thenReturn(new PageImpl<>(List.of(matricula), pageable, 1));

        MatriculaAdminListResponse fila = service.listarMatriculas(
                null, null, null, null, null, null, pageable).getContent().get(0);

        assertFalse(fila.puedeEditar());
        assertFalse(fila.puedeEliminar());
        assertEquals("Solo se pueden editar matrículas PENDIENTE.", fila.motivoNoEditar());
        assertEquals("Solo se pueden eliminar matrículas PENDIENTE.", fila.motivoNoEliminar());
    }

    @Test
    void listarMatriculas_pendienteConPago_noPermiteEliminar() {
        autenticar(crearAdmin());

        Seccion seccion = crearSeccion();
        Matricula matricula = new Matricula();
        matricula.setId(100L);
        matricula.setFechaAsignacion(LocalDate.now());
        matricula.setEstado(EstadoMatricula.PENDIENTE);
        matricula.setEstudiante(crearEstudiante());
        matricula.setSeccion(seccion);

        CompromisoPago compromiso = new CompromisoPago();
        compromiso.setId(10L);
        compromiso.setTipoCompromiso(TipoCompromiso.MATRICULA);
        compromiso.setEstado(EstadoCompromisoPago.PENDIENTE);
        compromiso.setMatricula(matricula);

        Pago pago = new Pago();
        pago.setCompromisoPago(compromiso);

        Pageable pageable = PageRequest.of(0, 10);
        when(matriculaRepository.listarMatriculas(
                null, null, null, null, null, "", pageable))
                .thenReturn(new PageImpl<>(List.of(matricula), pageable, 1));
        when(periodoMatriculaRepository.findAll())
                .thenReturn(List.of(crearPeriodo(seccion, true)));
        when(compromisoPagoRepository.findByMatriculaIdIn(any()))
                .thenReturn(List.of(compromiso));
        when(pagoRepository.findByCompromisoPagoIdIn(any()))
                .thenReturn(List.of(pago));

        MatriculaAdminListResponse fila = service.listarMatriculas(
                null, null, null, null, null, null, pageable).getContent().get(0);

        assertTrue(fila.puedeEditar());
        assertFalse(fila.puedeEliminar());
        assertEquals("La matrícula tiene pagos registrados.", fila.motivoNoEliminar());
    }
}