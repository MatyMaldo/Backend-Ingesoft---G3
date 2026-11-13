package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.operaciones.ProgresoLote;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.ApoderadoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioLoteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioLoteRepository.DatosApoderado;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioLoteRepository.DatosEstudiante;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioLoteRepository.NuevoUsuario;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaUsuariosRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaUsuariosRequest.Fila;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaUsuariosRequest.TipoUsuario;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ResultadoFilaUsuario;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La carga masiva de usuarios valida la unicidad en bloque (pocas consultas), resuelve cada fila por separado y
 * guarda las válidas por lotes, conservando los mismos mensajes del registro individual.
 */
@ExtendWith(MockitoExtension.class)
class CargaMasivaUsuariosServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ApoderadoRepository apoderadoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioLoteRepository usuarioLoteRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private AuditoriaService auditoriaService;

    private static final Long ADMIN_ID = 7L;

    private CargaMasivaUsuariosService servicio;
    private Grabador progreso;
    /** Ids que "asigna la BD" a los usuarios insertados, en orden. */
    private final AtomicLong siguienteId = new AtomicLong(100);

    @BeforeEach
    void preparar() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        servicio = new CargaMasivaUsuariosService(usuarioRepository, apoderadoRepository, estudianteRepository,
                usuarioLoteRepository, auditoriaService, validator, transactionManager);
        progreso = new Grabador();
        // Por omisión nada existe aún en la BD.
        lenient().when(usuarioRepository.findDnisExistentesPorTipo(anyCollection(), any())).thenReturn(List.of());
        lenient().when(usuarioRepository.findEmailsExistentes(anyCollection())).thenReturn(List.of());
        lenient().when(usuarioRepository.findUsernamesExistentes(anyCollection())).thenReturn(List.of());
        lenient().when(estudianteRepository.findCodigosExistentes(anyCollection())).thenReturn(List.of());
        lenient().when(apoderadoRepository.findActivosPorDni(anyCollection())).thenReturn(List.of());
        lenient().when(usuarioLoteRepository.insertarUsuarios(any())).thenAnswer(invocacion -> {
            List<NuevoUsuario> usuarios = invocacion.getArgument(0);
            if (usuarios == null) {
                // Mockito invoca la respuesta con nulo al registrar un matcher en otro stubbing.
                return List.of();
            }
            return usuarios.stream().map(u -> siguienteId.getAndIncrement()).toList();
        });
    }

    private static Fila apoderado(int linea, String dni, String email) {
        return new Fila(linea, TipoUsuario.APODERADO, "Rosa", "Quispe", "Mamani", dni, email, "981001001", 41,
                null, null, null);
    }

    private static Fila docente(int linea, String dni, String email) {
        return new Fila(linea, TipoUsuario.DOCENTE, "Luis", "Huamán", null, dni, email, null, null, null, null, null);
    }

    private static Fila estudiante(int linea, String dni, String email, String dniApoderado, String codigo) {
        return new Fila(linea, TipoUsuario.ESTUDIANTE, "Ana", "Pérez", null, dni, email, null, null,
                LocalDate.of(2012, 5, 10), dniApoderado, codigo);
    }

    private static CargaMasivaUsuariosRequest lote(Fila... filas) {
        return new CargaMasivaUsuariosRequest(List.of(filas));
    }

    private static Apoderado apoderadoConDni(long id, String dni) {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(id);
        apoderado.setDni(dni);
        return apoderado;
    }

    private ResultadoFilaUsuario resultadoDe(int linea) {
        return progreso.resultado.stream().filter(r -> r.linea() == linea).findFirst().orElseThrow();
    }

    /** Hace fallar la inserción de usuarios cuando el lote incluye alguno de esos correos. */
    private void fallaAlInsertarCorreo(String correo) {
        lenient().when(usuarioLoteRepository.insertarUsuarios(argThat(
                usuarios -> usuarios != null && usuarios.stream().anyMatch(u -> correo.equals(u.email())))))
                .thenThrow(new IllegalStateException("violación UNIQUE"));
    }

    @Test
    void fallaDeConexionAlGuardar_seReintentaElLoteEnteroUnaVezSinCaerAFilaPorFila() {
        AtomicInteger intentos = new AtomicInteger();
        lenient().when(usuarioLoteRepository.insertarUsuarios(any())).thenAnswer(invocacion -> {
            List<NuevoUsuario> usuarios = invocacion.getArgument(0);
            if (usuarios == null) {
                return List.of();
            }
            if (intentos.getAndIncrement() == 0) {
                throw new DataAccessResourceFailureException("No operations allowed after connection closed.");
            }
            return usuarios.stream().map(u -> siguienteId.getAndIncrement()).toList();
        });

        servicio.cargar(lote(
                apoderado(2, "71000001", "rosa@x.com"),
                docente(3, "72000001", "luis@x.com")), ADMIN_ID, progreso);

        assertTrue(progreso.resultado.stream().allMatch(r -> ResultadoFilaUsuario.CREADO.equals(r.estado())));
        // Un intento fallido y uno bueno, los dos con el lote completo (no una llamada por fila).
        assertEquals(2, intentos.get());
    }

    @Test
    void fallaDeConexionPersistente_terminaLaOperacionSinReintentarFilaPorFila() {
        lenient().when(usuarioLoteRepository.insertarUsuarios(argThat(usuarios -> usuarios != null)))
                .thenThrow(new DataAccessResourceFailureException("No operations allowed after connection closed."));

        assertThrows(DataAccessResourceFailureException.class, () -> servicio.cargar(lote(
                apoderado(2, "71000001", "rosa@x.com"),
                docente(3, "72000001", "luis@x.com")), ADMIN_ID, progreso));

        // Solo el intento inicial y el reintento del lote: nada de una transacción por fila.
        verify(usuarioLoteRepository, times(2)).insertarUsuarios(any());
    }

    @Test
    void filasValidas_seCreanTodasYElEstudianteQuedaVinculadoAlApoderadoDelArchivo() {
        servicio.cargar(lote(
                apoderado(2, "71000001", "rosa@x.com"),
                docente(3, "72000001", "luis@x.com"),
                estudiante(4, "73000001", "ana@x.com", "71000001", "EST001")), ADMIN_ID, progreso);

        assertEquals(3, progreso.resultado.size());
        assertTrue(progreso.resultado.stream().allMatch(r -> ResultadoFilaUsuario.CREADO.equals(r.estado())));
        assertEquals("EST001", resultadoDe(4).codigo());
        assertNull(resultadoDe(2).codigo());

        // Primero apoderados y docentes (ids 100 y 101), luego el estudiante (id 102) vinculado al apoderado 100.
        ArgumentCaptor<List<DatosEstudiante>> estudiantes = ArgumentCaptor.forClass(List.class);
        verify(usuarioLoteRepository).insertarEstudiantes(estudiantes.capture());
        assertEquals(List.of(new DatosEstudiante(102L, "EST001", LocalDate.of(2012, 5, 10), 100L)),
                estudiantes.getValue());
        verify(usuarioLoteRepository).insertarApoderados(List.of(new DatosApoderado(100L, 41)));
        verify(usuarioLoteRepository).insertarDocentes(List.of(101L));
        assertEquals(3, progreso.total);
        assertEquals(List.of("VALIDANDO", "GUARDANDO"), progreso.fases);
    }

    @Test
    void losDatosDeCadaUsuarioSeEnviananNormalizados() {
        servicio.cargar(lote(new Fila(2, TipoUsuario.DOCENTE, "Luis", "Huamán", "  ", "72000001", "luis@x.com", "",
                null, null, null, null)), ADMIN_ID, progreso);

        ArgumentCaptor<List<NuevoUsuario>> captura = ArgumentCaptor.forClass(List.class);
        verify(usuarioLoteRepository).insertarUsuarios(captura.capture());
        assertEquals(List.of(new NuevoUsuario("DOCENTE", "Luis", "Huamán", null, "72000001", "luis", "luis@x.com", null)),
                captura.getValue());
    }

    @Test
    void laUnicidadSeVerificaEnBloque_sinConsultasPorFila() {
        servicio.cargar(lote(
                docente(2, "72000001", "a@x.com"), docente(3, "72000002", "b@x.com"),
                docente(4, "72000003", "c@x.com")), ADMIN_ID, progreso);

        verify(usuarioRepository, times(1)).findDnisExistentesPorTipo(anyCollection(), eq(Docente.class));
        verify(usuarioRepository, times(1)).findEmailsExistentes(anyCollection());
        verify(usuarioRepository, times(1)).findUsernamesExistentes(anyCollection());
        verify(usuarioRepository, never()).existsByDniAndTipoUsuario(any(), any());
        verify(usuarioRepository, never()).existsByEmail(any());
        // Los tres docentes viajan en una sola inserción.
        verify(usuarioLoteRepository, times(1)).insertarUsuarios(any());
        verify(usuarioLoteRepository).insertarDocentes(List.of(100L, 101L, 102L));
    }

    @Test
    void unaFilaInvalidaSeRechazaConSuMotivoYElRestoContinua() {
        servicio.cargar(lote(docente(2, "123", "a@x.com"), docente(3, "72000002", "b@x.com")), ADMIN_ID, progreso);

        assertEquals(ResultadoFilaUsuario.RECHAZADO, resultadoDe(2).estado());
        assertEquals("El DNI debe tener exactamente 8 dígitos.", resultadoDe(2).motivo());
        assertEquals(ResultadoFilaUsuario.CREADO, resultadoDe(3).estado());
        verify(usuarioLoteRepository).insertarDocentes(List.of(100L));
        assertEquals(List.of("Fila 2: El DNI debe tener exactamente 8 dígitos."), progreso.errores);
    }

    @Test
    void unDuplicadoDentroDelArchivoSeRechaza_yElPrimeroSeConserva() {
        servicio.cargar(lote(
                docente(2, "72000001", "a@x.com"),
                docente(3, "72000001", "b@x.com"),
                docente(4, "72000009", "A@x.com"),
                docente(5, "72000010", "c@x.com")), ADMIN_ID, progreso);

        assertEquals(ResultadoFilaUsuario.CREADO, resultadoDe(2).estado());
        assertEquals("Ya existe un docente registrado con el DNI indicado.", resultadoDe(3).motivo());
        assertEquals("Ya existe un usuario registrado con el correo indicado.", resultadoDe(4).motivo());
        assertEquals(ResultadoFilaUsuario.CREADO, resultadoDe(5).estado());
        verify(usuarioLoteRepository).insertarDocentes(List.of(100L, 101L));
    }

    @Test
    void unaFilaRechazadaNoOcupaSuDniNiSuCorreo() {
        // La fila 2 se rechaza (teléfono inválido); la 3 usa el mismo DNI y correo y debe poder crearse.
        Fila invalida = new Fila(2, TipoUsuario.DOCENTE, "Luis", "Huamán", null, "72000001", "a@x.com", "12", null,
                null, null, null);

        servicio.cargar(lote(invalida, docente(3, "72000001", "a@x.com")), ADMIN_ID, progreso);

        assertEquals(ResultadoFilaUsuario.RECHAZADO, resultadoDe(2).estado());
        assertEquals(ResultadoFilaUsuario.CREADO, resultadoDe(3).estado());
    }

    @Test
    void loQueYaExisteEnLaBase_seRechazaConLosMensajesDelRegistroIndividual() {
        when(usuarioRepository.findDnisExistentesPorTipo(anyCollection(), eq(Docente.class)))
                .thenReturn(List.of("72000001"));
        when(usuarioRepository.findEmailsExistentes(anyCollection())).thenReturn(List.of("Dos@X.com"));
        when(usuarioRepository.findUsernamesExistentes(anyCollection())).thenReturn(List.of("tres"));

        servicio.cargar(lote(
                docente(2, "72000001", "uno@x.com"),
                docente(3, "72000002", "dos@x.com"),
                docente(4, "72000003", "tres@otro.com"),
                docente(5, "72000004", "cuatro@x.com")), ADMIN_ID, progreso);

        assertEquals("Ya existe un docente registrado con el DNI indicado.", resultadoDe(2).motivo());
        assertEquals("Ya existe un usuario registrado con el correo indicado.", resultadoDe(3).motivo());
        assertEquals("Ya existe un usuario registrado con ese identificador.", resultadoDe(4).motivo());
        assertEquals(ResultadoFilaUsuario.CREADO, resultadoDe(5).estado());
    }

    @Test
    void elDniSoloChocaConUsuariosDelMismoTipo() {
        // El DNI 71000001 existe como apoderado: el docente no choca, pero el apoderado del archivo sí.
        when(usuarioRepository.findDnisExistentesPorTipo(anyCollection(), eq(Apoderado.class)))
                .thenReturn(List.of("71000001"));

        servicio.cargar(lote(docente(2, "71000001", "a@x.com"), apoderado(3, "71000001", "b@x.com")), ADMIN_ID, progreso);

        assertEquals(ResultadoFilaUsuario.CREADO, resultadoDe(2).estado());
        assertEquals("Ya existe un apoderado registrado con el DNI indicado.", resultadoDe(3).motivo());
    }

    @Test
    void unCodigoDeEstudianteYaUsadoSeRechaza() {
        when(estudianteRepository.findCodigosExistentes(anyCollection())).thenReturn(List.of("EST001"));
        when(apoderadoRepository.findActivosPorDni(anyCollection())).thenReturn(List.of(apoderadoConDni(7L, "71000001")));

        servicio.cargar(lote(estudiante(2, "73000001", "ana@x.com", "71000001", "EST001")), ADMIN_ID, progreso);

        assertEquals("Ya existe un estudiante registrado con ese código.", resultadoDe(2).motivo());
    }

    @Test
    void elEstudianteSeVinculaAUnApoderadoActivoQueYaExisteEnElSistema() {
        when(apoderadoRepository.findActivosPorDni(anyCollection())).thenReturn(List.of(apoderadoConDni(7L, "71000001")));

        servicio.cargar(lote(estudiante(2, "73000001", "ana@x.com", "71000001", "EST001")), ADMIN_ID, progreso);

        assertEquals(ResultadoFilaUsuario.CREADO, resultadoDe(2).estado());
        verify(usuarioLoteRepository).insertarEstudiantes(
                List.of(new DatosEstudiante(100L, "EST001", LocalDate.of(2012, 5, 10), 7L)));
    }

    @Test
    void unEstudianteSinApoderadoConocidoSeRechaza() {
        servicio.cargar(lote(estudiante(2, "73000001", "ana@x.com", "71999999", "EST001")), ADMIN_ID, progreso);

        assertEquals("No se encontró un apoderado activo con el DNI 71999999.", resultadoDe(2).motivo());
        verify(usuarioLoteRepository, never()).insertarUsuarios(any());
    }

    @Test
    void elApoderadoDelArchivoCuentaAunqueSuFilaVengaDespuesDelEstudiante() {
        servicio.cargar(lote(
                estudiante(2, "73000001", "ana@x.com", "71000001", "EST001"),
                apoderado(3, "71000001", "rosa@x.com")), ADMIN_ID, progreso);

        assertEquals(ResultadoFilaUsuario.CREADO, resultadoDe(2).estado());
        assertEquals(ResultadoFilaUsuario.CREADO, resultadoDe(3).estado());
        verify(usuarioLoteRepository).insertarEstudiantes(
                List.of(new DatosEstudiante(101L, "EST001", LocalDate.of(2012, 5, 10), 100L)));
    }

    @Test
    void siElApoderadoDelArchivoEsRechazado_suEstudianteTambien() {
        // Apoderado con correo inválido: se rechaza, y su estudiante ya no tiene a quién vincularse.
        Fila apoderadoMalo = new Fila(2, TipoUsuario.APODERADO, "Rosa", "Quispe", null, "71000001", "sin-arroba", null,
                41, null, null, null);

        servicio.cargar(lote(apoderadoMalo, estudiante(3, "73000001", "ana@x.com", "71000001", "EST001")), ADMIN_ID, progreso);

        assertEquals(ResultadoFilaUsuario.RECHAZADO, resultadoDe(2).estado());
        assertEquals("No se encontró un apoderado activo con el DNI 71000001.", resultadoDe(3).motivo());
    }

    @Test
    void siUnLoteFallaAlGuardar_seReintentaFilaPorFilaYSoloSeRechazaLaCulpable() {
        fallaAlInsertarCorreo("b@x.com");

        servicio.cargar(lote(
                docente(2, "72000001", "a@x.com"), docente(3, "72000002", "b@x.com"),
                docente(4, "72000003", "c@x.com")), ADMIN_ID, progreso);

        assertEquals(ResultadoFilaUsuario.CREADO, resultadoDe(2).estado());
        assertEquals(ResultadoFilaUsuario.RECHAZADO, resultadoDe(3).estado());
        assertTrue(resultadoDe(3).motivo().startsWith("No se pudo registrar el usuario"));
        assertEquals(ResultadoFilaUsuario.CREADO, resultadoDe(4).estado());
        assertEquals(2, progreso.exitosos);
        assertEquals(1, progreso.errorescontados);
    }

    @Test
    void siElApoderadoDelArchivoNoSePudoGuardar_suEstudianteSeRechazaConUnMotivoClaro() {
        fallaAlInsertarCorreo("rosa@x.com");

        servicio.cargar(lote(
                apoderado(2, "71000001", "rosa@x.com"),
                estudiante(3, "73000001", "ana@x.com", "71000001", "EST001")), ADMIN_ID, progreso);

        assertEquals(ResultadoFilaUsuario.RECHAZADO, resultadoDe(2).estado());
        assertEquals("El apoderado con DNI 71000001 no se pudo registrar.", resultadoDe(3).motivo());
        verify(usuarioLoteRepository, never()).insertarEstudiantes(any());
    }

    @Test
    void elResultadoSalePorLineaYElAvanceCuentaCreadasYRechazadas() {
        servicio.cargar(lote(
                docente(5, "72000001", "a@x.com"), docente(3, "999", "b@x.com"),
                docente(4, "72000003", "c@x.com")), ADMIN_ID, progreso);

        assertEquals(List.of(3, 4, 5), progreso.resultado.stream().map(ResultadoFilaUsuario::linea).toList());
        assertEquals(3, progreso.procesados);
        assertEquals(2, progreso.exitosos);
        assertEquals(1, progreso.errorescontados);
    }

    @Test
    void unArchivoGrandeSeGuardaEnLotes() {
        int total = CargaMasivaUsuariosService.TAMANO_LOTE * 2 + 5;
        Fila[] filas = IntStream.range(0, total)
                .mapToObj(i -> docente(i + 2, String.format("%08d", 72000000 + i), "doc" + i + "@x.com"))
                .toArray(Fila[]::new);

        servicio.cargar(lote(filas), ADMIN_ID, progreso);

        verify(usuarioLoteRepository, times(3)).insertarUsuarios(any());
        verify(usuarioLoteRepository, times(3)).insertarDocentes(any());
        assertEquals(total, progreso.exitosos);
        // Una publicación tras validar y una por lote guardado (3 lotes).
        assertEquals(4, progreso.actualizaciones);
    }

    @Test
    void dejaUnRegistroDeAuditoriaResumenAnombreDelAdministrador() {
        servicio.cargar(lote(
                apoderado(2, "71000001", "rosa@x.com"),
                docente(3, "72000001", "luis@x.com"),
                docente(4, "123", "mal@x.com"),
                estudiante(5, "73000001", "ana@x.com", "71000001", "EST001")), ADMIN_ID, progreso);

        ArgumentCaptor<RegistrarAuditoriaRequestDTO> captura = ArgumentCaptor.forClass(RegistrarAuditoriaRequestDTO.class);
        verify(auditoriaService, times(1)).registrarAuditoria(captura.capture());
        RegistrarAuditoriaRequestDTO registro = captura.getValue();
        assertEquals(ADMIN_ID, registro.getUsuarioId());
        assertEquals(CategoriaAuditoria.USUARIO, registro.getCategoria());
        assertEquals(TipoAccionAuditoria.CREAR, registro.getAccion());
        assertEquals("Usuario", registro.getEntidad());
        assertEquals("Carga masiva de usuarios: 3 creado(s) (1 apoderado(s), 1 docente(s), 1 estudiante(s)), "
                + "1 fila(s) rechazada(s)", registro.getDescripcion());
    }

    @Test
    void sinRechazos_laDescripcionNoMencionaFilasRechazadas() {
        servicio.cargar(lote(docente(2, "72000001", "a@x.com")), ADMIN_ID, progreso);

        ArgumentCaptor<RegistrarAuditoriaRequestDTO> captura = ArgumentCaptor.forClass(RegistrarAuditoriaRequestDTO.class);
        verify(auditoriaService).registrarAuditoria(captura.capture());
        assertEquals("Carga masiva de usuarios: 1 creado(s) (0 apoderado(s), 1 docente(s), 0 estudiante(s))",
                captura.getValue().getDescripcion());
    }

    @Test
    void sinNingunUsuarioCreado_noSeAudita() {
        servicio.cargar(lote(docente(2, "123", "a@x.com")), ADMIN_ID, progreso);

        verify(auditoriaService, never()).registrarAuditoria(any());
    }

    @Test
    void sinProgreso_noFalla() {
        servicio.cargar(lote(docente(2, "72000001", "a@x.com")), ADMIN_ID, null);

        verify(usuarioLoteRepository).insertarDocentes(List.of(100L));
    }

    /** Registra lo que la operación publica. */
    private static class Grabador implements ProgresoLote {
        int total = -1;
        int procesados;
        int exitosos;
        int errorescontados;
        int actualizaciones;
        final List<String> fases = new ArrayList<>();
        final List<String> errores = new ArrayList<>();
        List<ResultadoFilaUsuario> resultado = List.of();

        @Override
        public void iniciar(int total) {
            this.total = total;
        }

        @Override
        public void actualizar(int procesados, int exitosos, int omitidos, int errores) {
            this.procesados = procesados;
            this.exitosos = exitosos;
            this.errorescontados = errores;
            this.actualizaciones++;
        }

        @Override
        public void registrarError(String mensaje) {
            errores.add(mensaje);
        }

        @Override
        public void fase(String mensaje) {
            fases.add(mensaje);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void resultado(Object resultado) {
            this.resultado = (List<ResultadoFilaUsuario>) resultado;
        }
    }
}
