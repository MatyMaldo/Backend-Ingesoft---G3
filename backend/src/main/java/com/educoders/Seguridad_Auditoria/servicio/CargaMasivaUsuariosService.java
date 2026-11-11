package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.operaciones.ProgresoLote;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.ApoderadoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioLoteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaUsuariosRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaUsuariosRequest.Fila;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaUsuariosRequest.TipoUsuario;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarApoderadoRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarDocenteRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarEstudianteRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ResultadoFilaUsuario;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Carga masiva de usuarios (apoderados, docentes y estudiantes) en una sola operación.
 * <p>
 * Reemplaza el registro fila por fila (una petición por fila, cada una con su autenticación, sus consultas de
 * unicidad y su commit): aquí la unicidad se verifica con unas pocas consultas para todo el archivo, se resuelve en
 * memoria y las filas válidas se guardan en transacciones de {@value #TAMANO_LOTE} filas.
 * <p>
 * Cada fila se resuelve por separado: una fila inválida o duplicada se rechaza con su motivo y el resto continúa. Si un
 * lote falla al guardar (por ejemplo, una carrera con otro registro que viola una restricción UNIQUE), se reintenta
 * fila por fila para rechazar solo la culpable.
 */
@Service
public class CargaMasivaUsuariosService {

    private static final Logger log = LoggerFactory.getLogger(CargaMasivaUsuariosService.class);

    /** Filas por transacción al guardar: acota lo que se pierde si un lote falla y permite informar el avance. */
    static final int TAMANO_LOTE = 100;

    private static final ProgresoLote SIN_PROGRESO = new ProgresoLote() {
        @Override
        public void iniciar(int total) {
        }

        @Override
        public void actualizar(int procesados, int exitosos, int omitidos, int errores) {
        }

        @Override
        public void registrarError(String mensaje) {
        }

        @Override
        public void fase(String mensaje) {
        }
    };

    private final UsuarioRepository usuarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioLoteRepository usuarioLoteRepository;
    private final AuditoriaService auditoriaService;
    private final Validator validator;
    private final TransactionTemplate transacciones;

    public CargaMasivaUsuariosService(UsuarioRepository usuarioRepository,
                                      ApoderadoRepository apoderadoRepository,
                                      EstudianteRepository estudianteRepository,
                                      UsuarioLoteRepository usuarioLoteRepository,
                                      AuditoriaService auditoriaService,
                                      Validator validator,
                                      PlatformTransactionManager transactionManager) {
        this.usuarioRepository = usuarioRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioLoteRepository = usuarioLoteRepository;
        this.auditoriaService = auditoriaService;
        this.validator = validator;
        this.transacciones = new TransactionTemplate(transactionManager);
    }

    /** Fila que superó las validaciones y espera guardarse. */
    private static final class Aceptada {
        final int posicion;
        final Fila fila;
        final RegistrarApoderadoRequest apoderado;
        final RegistrarDocenteRequest docente;
        final RegistrarEstudianteRequest estudiante;
        /** Apoderado del estudiante: su DNI, y su id si ya existía antes de la carga. */
        final String dniApoderado;
        final Long idApoderadoExistente;
        /** Id del apoderado una vez guardado (solo apoderados del archivo). */
        Long idGuardado;

        Aceptada(int posicion, Fila fila, RegistrarApoderadoRequest apoderado, RegistrarDocenteRequest docente,
                 RegistrarEstudianteRequest estudiante, String dniApoderado, Long idApoderadoExistente) {
            this.posicion = posicion;
            this.fila = fila;
            this.apoderado = apoderado;
            this.docente = docente;
            this.estudiante = estudiante;
            this.dniApoderado = dniApoderado;
            this.idApoderadoExistente = idApoderadoExistente;
        }
    }

    /**
     * Procesa todo el archivo y publica el resultado de cada fila con {@link ProgresoLote#resultado(Object)}.
     *
     * @param solicitud filas ya leídas del archivo
     * @param usuarioId administrador que lanza la carga; queda como autor del registro de auditoría
     * @param progreso  avance de la operación; puede ser {@code null}
     */
    public void cargar(CargaMasivaUsuariosRequest solicitud, Long usuarioId, ProgresoLote progreso) {
        ProgresoLote avance = progreso != null ? progreso : SIN_PROGRESO;
        List<Fila> filas = solicitud.filas();
        ResultadoFilaUsuario[] resultados = new ResultadoFilaUsuario[filas.size()];
        int[] rechazadas = {0};

        avance.iniciar(filas.size());
        avance.fase("VALIDANDO");

        List<Aceptada> aceptadas = validar(filas, resultados, rechazadas, avance);
        avance.actualizar(rechazadas[0], 0, 0, rechazadas[0]);

        avance.fase("GUARDANDO");
        // Id de cada apoderado del archivo ya guardado, por su DNI, para vincularle sus estudiantes.
        Map<String, Long> idPorDniApoderado = new HashMap<>();
        int[] creadas = {0};

        // Los estudiantes necesitan el id de su apoderado: primero apoderados y docentes, luego estudiantes.
        List<Aceptada> primera = aceptadas.stream().filter(a -> a.estudiante == null).toList();
        List<Aceptada> segunda = aceptadas.stream().filter(a -> a.estudiante != null).toList();

        guardar(primera, idPorDniApoderado, resultados, rechazadas, creadas, avance);
        guardar(segunda, idPorDniApoderado, resultados, rechazadas, creadas, avance);

        auditar(usuarioId, filas, resultados);

        List<ResultadoFilaUsuario> ordenados = new ArrayList<>(List.of(resultados));
        ordenados.sort(Comparator.comparingInt(ResultadoFilaUsuario::linea));
        avance.resultado(ordenados);
    }

    /**
     * Deja UN registro de auditoría de la carga (HU-SAT-04, categoría Usuarios), a nombre de quien la lanzó, con cuántos
     * usuarios se crearon por tipo y cuántas filas se rechazaron. Si no se creó ninguno, no cambió nada y no se registra.
     * Una falla al auditar no afecta a la carga: el servicio de auditoría la registra en el log y sigue.
     */
    private void auditar(Long usuarioId, List<Fila> filas, ResultadoFilaUsuario[] resultados) {
        Map<TipoUsuario, Integer> creados = new EnumMap<>(TipoUsuario.class);
        int rechazados = 0;
        for (int i = 0; i < filas.size(); i++) {
            if (ResultadoFilaUsuario.CREADO.equals(resultados[i].estado())) {
                creados.merge(filas.get(i).tipoUsuario(), 1, Integer::sum);
            } else {
                rechazados++;
            }
        }
        int total = creados.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return;
        }

        String descripcion = "Carga masiva de usuarios: " + total + " creado(s) ("
                + creados.getOrDefault(TipoUsuario.APODERADO, 0) + " apoderado(s), "
                + creados.getOrDefault(TipoUsuario.DOCENTE, 0) + " docente(s), "
                + creados.getOrDefault(TipoUsuario.ESTUDIANTE, 0) + " estudiante(s))"
                + (rechazados > 0 ? ", " + rechazados + " fila(s) rechazada(s)" : "");
        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                usuarioId, CategoriaAuditoria.USUARIO, TipoAccionAuditoria.CREAR, "Usuario", null, descripcion, List.of()));
    }

    // ───────────────────────────── validación ─────────────────────────────

    private List<Aceptada> validar(List<Fila> filas, ResultadoFilaUsuario[] resultados, int[] rechazadas,
                                   ProgresoLote avance) {
        Existentes existentes = consultarExistentes(filas);
        Map<TipoUsuario, Set<String>> dnisUsados = new EnumMap<>(TipoUsuario.class);
        for (TipoUsuario tipo : TipoUsuario.values()) {
            dnisUsados.put(tipo, new HashSet<>(existentes.dnis.get(tipo)));
        }
        Set<String> emailsUsados = new HashSet<>(existentes.emails);
        Set<String> usernamesUsados = new HashSet<>(existentes.usernames);
        Set<String> codigosUsados = new HashSet<>(existentes.codigos);
        // Apoderados aceptados del propio archivo (por DNI), para vincular a los estudiantes que los citan.
        Set<String> apoderadosDelArchivo = new HashSet<>();

        List<Aceptada> aceptadas = new ArrayList<>();

        // Estudiantes al final: así el apoderado del archivo cuenta aunque su fila venga después de la del estudiante.
        List<Integer> orden = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            if (filas.get(i).tipoUsuario() != TipoUsuario.ESTUDIANTE) {
                orden.add(i);
            }
        }
        for (int i = 0; i < filas.size(); i++) {
            if (filas.get(i).tipoUsuario() == TipoUsuario.ESTUDIANTE) {
                orden.add(i);
            }
        }

        for (int posicion : orden) {
            Fila fila = filas.get(posicion);
            Candidata candidata = construir(posicion, fila, existentes, apoderadosDelArchivo);
            String motivo = candidata.motivoRechazo();
            Aceptada aceptada = null;
            if (motivo == null) {
                motivo = chocaConExistentes(candidata.aceptada(), dnisUsados, emailsUsados, usernamesUsados, codigosUsados);
                if (motivo == null) {
                    aceptada = candidata.aceptada();
                }
            }

            if (aceptada != null) {
                registrarUsados(aceptada, dnisUsados, emailsUsados, usernamesUsados, codigosUsados);
                if (fila.tipoUsuario() == TipoUsuario.APODERADO) {
                    apoderadosDelArchivo.add(aceptada.apoderado.dni());
                }
                aceptadas.add(aceptada);
            } else {
                rechazar(resultados, posicion, fila, motivo, rechazadas, avance);
            }
        }
        return aceptadas;
    }

    /** Resultado de armar una fila: la fila aceptable o el motivo por el que no lo es. */
    private record Candidata(Aceptada aceptada, String motivoRechazo) {
    }

    private Candidata construir(int posicion, Fila fila, Existentes existentes, Set<String> apoderadosDelArchivo) {
        String apellidoMaterno = normalizarOpcional(fila.apellidoMaterno());
        String telefono = normalizarOpcional(fila.telefono());

        return switch (fila.tipoUsuario()) {
            case APODERADO -> {
                RegistrarApoderadoRequest solicitud = new RegistrarApoderadoRequest(fila.nombre(),
                        fila.apellidoPaterno(), apellidoMaterno, fila.dni(), fila.email(), telefono, fila.edad());
                String error = primerError(solicitud);
                yield error != null ? new Candidata(null, error)
                        : new Candidata(new Aceptada(posicion, fila, solicitud, null, null, null, null), null);
            }
            case DOCENTE -> {
                RegistrarDocenteRequest solicitud = new RegistrarDocenteRequest(fila.nombre(), fila.apellidoPaterno(),
                        apellidoMaterno, fila.dni(), fila.email(), telefono);
                String error = primerError(solicitud);
                yield error != null ? new Candidata(null, error)
                        : new Candidata(new Aceptada(posicion, fila, null, solicitud, null, null, null), null);
            }
            case ESTUDIANTE -> {
                // El id del apoderado se resuelve abajo: aquí va un valor cualquiera solo para pasar la validación.
                RegistrarEstudianteRequest solicitud = new RegistrarEstudianteRequest(fila.nombre(),
                        fila.apellidoPaterno(), apellidoMaterno, fila.dni(), fila.email(), telefono, fila.codigo(),
                        fila.fechaNacimiento(), 0L);
                String error = primerError(solicitud);
                if (error != null) {
                    yield new Candidata(null, error);
                }
                String dniApoderado = normalizarOpcional(fila.dniApoderado());
                Long idExistente = dniApoderado == null ? null : existentes.apoderados.get(dniApoderado);
                boolean delArchivo = dniApoderado != null && apoderadosDelArchivo.contains(dniApoderado);
                if (dniApoderado == null || (idExistente == null && !delArchivo)) {
                    yield new Candidata(null, dniApoderado == null
                            ? "El DNI del apoderado es obligatorio."
                            : "No se encontró un apoderado activo con el DNI " + dniApoderado + ".");
                }
                // Si el apoderado viene en el archivo, se prefiere ese (el del sistema solo se usa si no está).
                yield new Candidata(new Aceptada(posicion, fila, null, null, solicitud, dniApoderado,
                        delArchivo ? null : idExistente), null);
            }
        };
    }

    /** Primer mensaje de validación del DTO (ordenado por campo, para que el resultado sea estable), o nulo. */
    private <T> String primerError(T solicitud) {
        Set<ConstraintViolation<T>> errores = validator.validate(solicitud);
        return errores.stream()
                .sorted(Comparator.comparing((ConstraintViolation<T> e) -> e.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(null);
    }

    /** Mismas unicidades y mensajes que el registro individual, resueltos en memoria. */
    private String chocaConExistentes(Aceptada aceptada, Map<TipoUsuario, Set<String>> dnisUsados,
                                      Set<String> emails, Set<String> usernames, Set<String> codigos) {
        TipoUsuario tipo = aceptada.fila.tipoUsuario();
        if (dnisUsados.get(tipo).contains(dniDe(aceptada))) {
            return "Ya existe " + (switch (tipo) {
                case APODERADO -> "un apoderado";
                case DOCENTE -> "un docente";
                case ESTUDIANTE -> "un estudiante";
            }) + " registrado con el DNI indicado.";
        }
        if (emails.contains(minusculas(emailDe(aceptada)))) {
            return "Ya existe un usuario registrado con el correo indicado.";
        }
        if (usernames.contains(minusculas(usernameDe(emailDe(aceptada))))) {
            return "Ya existe un usuario registrado con ese identificador.";
        }
        if (aceptada.estudiante != null && codigos.contains(aceptada.estudiante.codigo())) {
            return "Ya existe un estudiante registrado con ese código.";
        }
        return null;
    }

    private void registrarUsados(Aceptada aceptada, Map<TipoUsuario, Set<String>> dnisUsados, Set<String> emails,
                                 Set<String> usernames, Set<String> codigos) {
        dnisUsados.get(aceptada.fila.tipoUsuario()).add(dniDe(aceptada));
        emails.add(minusculas(emailDe(aceptada)));
        usernames.add(minusculas(usernameDe(emailDe(aceptada))));
        if (aceptada.estudiante != null) {
            codigos.add(aceptada.estudiante.codigo());
        }
    }

    // ───────────────────────────── consultas previas ─────────────────────────────

    /** Lo que ya existe en la BD de lo que trae el archivo, en pocas consultas para todo el lote. */
    private record Existentes(Map<TipoUsuario, Set<String>> dnis, Set<String> emails, Set<String> usernames,
                              Set<String> codigos, Map<String, Long> apoderados) {
    }

    private Existentes consultarExistentes(List<Fila> filas) {
        Map<TipoUsuario, Set<String>> dnis = new EnumMap<>(TipoUsuario.class);
        for (TipoUsuario tipo : TipoUsuario.values()) {
            Set<String> delTipo = filas.stream()
                    .filter(f -> f.tipoUsuario() == tipo)
                    .map(Fila::dni)
                    .filter(valor -> valor != null && !valor.isBlank())
                    .collect(Collectors.toSet());
            Set<String> existentes = new HashSet<>();
            if (!delTipo.isEmpty()) {
                existentes.addAll(usuarioRepository.findDnisExistentesPorTipo(delTipo, claseDe(tipo)));
            }
            dnis.put(tipo, existentes);
        }

        Set<String> correos = filas.stream().map(Fila::email).filter(CargaMasivaUsuariosService::tieneTexto)
                .collect(Collectors.toSet());
        Set<String> usernames = correos.stream().map(CargaMasivaUsuariosService::usernameDe)
                .filter(CargaMasivaUsuariosService::tieneTexto).collect(Collectors.toSet());
        Set<String> codigos = filas.stream().filter(f -> f.tipoUsuario() == TipoUsuario.ESTUDIANTE)
                .map(Fila::codigo).filter(CargaMasivaUsuariosService::tieneTexto).collect(Collectors.toSet());
        Set<String> dnisApoderados = filas.stream().filter(f -> f.tipoUsuario() == TipoUsuario.ESTUDIANTE)
                .map(f -> normalizarOpcional(f.dniApoderado())).filter(valor -> valor != null)
                .collect(Collectors.toSet());

        Set<String> emailsExistentes = correos.isEmpty() ? Set.of()
                : enMinusculas(usuarioRepository.findEmailsExistentes(correos));
        Set<String> usernamesExistentes = usernames.isEmpty() ? Set.of()
                : enMinusculas(usuarioRepository.findUsernamesExistentes(usernames));
        Set<String> codigosExistentes = codigos.isEmpty() ? Set.of()
                : new HashSet<>(estudianteRepository.findCodigosExistentes(codigos));
        Map<String, Long> apoderados = new HashMap<>();
        if (!dnisApoderados.isEmpty()) {
            for (Apoderado apoderado : apoderadoRepository.findActivosPorDni(dnisApoderados)) {
                apoderados.put(apoderado.getDni(), apoderado.getId());
            }
        }
        return new Existentes(dnis, emailsExistentes, usernamesExistentes, codigosExistentes, apoderados);
    }

    // ───────────────────────────── guardado ─────────────────────────────

    private void guardar(List<Aceptada> pendientes, Map<String, Long> idPorDniApoderado,
                         ResultadoFilaUsuario[] resultados, int[] rechazadas, int[] creadas, ProgresoLote avance) {
        // Un estudiante cuyo apoderado del archivo no llegó a guardarse no tiene a quién vincularse.
        List<Aceptada> porGuardar = new ArrayList<>();
        for (Aceptada a : pendientes) {
            if (a.estudiante != null && idApoderadoDe(a, idPorDniApoderado) == null) {
                rechazar(resultados, a.posicion, a.fila,
                        "El apoderado con DNI " + a.dniApoderado + " no se pudo registrar.", rechazadas, avance);
            } else {
                porGuardar.add(a);
            }
        }

        for (int desde = 0; desde < porGuardar.size(); desde += TAMANO_LOTE) {
            List<Aceptada> lote = porGuardar.subList(desde, Math.min(desde + TAMANO_LOTE, porGuardar.size()));

            boolean guardadoEnConjunto = true;
            try {
                transacciones.executeWithoutResult(estado -> guardarLote(lote, idPorDniApoderado));
            } catch (RuntimeException ex) {
                guardadoEnConjunto = false;
                log.warn("Falló el guardado de un lote de {} usuarios; se reintenta fila por fila: {}",
                        lote.size(), ex.getMessage());
            }

            if (guardadoEnConjunto) {
                for (Aceptada a : lote) {
                    marcarCreada(a, resultados, creadas, idPorDniApoderado);
                }
            } else {
                for (Aceptada a : lote) {
                    try {
                        transacciones.executeWithoutResult(estado -> guardarLote(List.of(a), idPorDniApoderado));
                        marcarCreada(a, resultados, creadas, idPorDniApoderado);
                    } catch (RuntimeException ex) {
                        log.warn("No se pudo guardar la fila {}: {}", a.fila.linea(), ex.getMessage());
                        rechazar(resultados, a.posicion, a.fila,
                                "No se pudo registrar el usuario. Verifica que el DNI y el correo no estén en uso.",
                                rechazadas, avance);
                    }
                }
            }
            avance.actualizar(rechazadas[0] + creadas[0], creadas[0], 0, rechazadas[0]);
        }
    }

    /** Guarda un lote dentro de la transacción en curso: una sentencia por tabla en vez de dos por usuario. */
    private void guardarLote(List<Aceptada> lote, Map<String, Long> idPorDniApoderado) {
        List<UsuarioLoteRepository.NuevoUsuario> nuevos =
                lote.stream().map(CargaMasivaUsuariosService::aNuevoUsuario).toList();
        List<Long> ids = usuarioLoteRepository.insertarUsuarios(nuevos);

        List<Long> docentes = new ArrayList<>();
        List<UsuarioLoteRepository.DatosApoderado> apoderados = new ArrayList<>();
        List<UsuarioLoteRepository.DatosEstudiante> estudiantes = new ArrayList<>();
        for (int i = 0; i < lote.size(); i++) {
            Aceptada a = lote.get(i);
            long id = ids.get(i);
            switch (a.fila.tipoUsuario()) {
                case DOCENTE -> docentes.add(id);
                case APODERADO -> {
                    apoderados.add(new UsuarioLoteRepository.DatosApoderado(id, a.apoderado.edad()));
                    a.idGuardado = id;
                }
                case ESTUDIANTE -> estudiantes.add(new UsuarioLoteRepository.DatosEstudiante(id, a.estudiante.codigo(),
                        a.estudiante.fechaNacimiento(), idApoderadoDe(a, idPorDniApoderado)));
            }
        }
        if (!docentes.isEmpty()) {
            usuarioLoteRepository.insertarDocentes(docentes);
        }
        if (!apoderados.isEmpty()) {
            usuarioLoteRepository.insertarApoderados(apoderados);
        }
        if (!estudiantes.isEmpty()) {
            usuarioLoteRepository.insertarEstudiantes(estudiantes);
        }
    }

    private static UsuarioLoteRepository.NuevoUsuario aNuevoUsuario(Aceptada a) {
        return switch (a.fila.tipoUsuario()) {
            case APODERADO -> new UsuarioLoteRepository.NuevoUsuario("APODERADO", a.apoderado.nombre(),
                    a.apoderado.apellidoPaterno(), a.apoderado.apellidoMaterno(), a.apoderado.dni(),
                    usernameDe(a.apoderado.email()), a.apoderado.email(), a.apoderado.telefono());
            case DOCENTE -> new UsuarioLoteRepository.NuevoUsuario("DOCENTE", a.docente.nombre(),
                    a.docente.apellidoPaterno(), a.docente.apellidoMaterno(), a.docente.dni(),
                    usernameDe(a.docente.email()), a.docente.email(), a.docente.telefono());
            case ESTUDIANTE -> new UsuarioLoteRepository.NuevoUsuario("ESTUDIANTE", a.estudiante.nombre(),
                    a.estudiante.apellidoPaterno(), a.estudiante.apellidoMaterno(), a.estudiante.dni(),
                    usernameDe(a.estudiante.email()), a.estudiante.email(), a.estudiante.telefono());
        };
    }

    /** Id del apoderado de un estudiante: el que ya existía en el sistema o el que se guardó desde este archivo. */
    private static Long idApoderadoDe(Aceptada a, Map<String, Long> idPorDniApoderado) {
        return a.idApoderadoExistente != null ? a.idApoderadoExistente : idPorDniApoderado.get(a.dniApoderado);
    }

    private void marcarCreada(Aceptada a, ResultadoFilaUsuario[] resultados, int[] creadas,
                              Map<String, Long> idPorDniApoderado) {
        creadas[0]++;
        resultados[a.posicion] = ResultadoFilaUsuario.creado(a.fila.linea(),
                a.estudiante != null ? a.estudiante.codigo() : null);
        if (a.idGuardado != null && a.apoderado != null) {
            idPorDniApoderado.put(a.apoderado.dni(), a.idGuardado);
        }
    }

    private void rechazar(ResultadoFilaUsuario[] resultados, int posicion, Fila fila, String motivo, int[] rechazadas,
                          ProgresoLote avance) {
        resultados[posicion] = ResultadoFilaUsuario.rechazado(fila.linea(), motivo);
        rechazadas[0]++;
        avance.registrarError("Fila " + fila.linea() + ": " + motivo);
    }

    // ───────────────────────────── utilidades ─────────────────────────────

    private static Class<? extends Usuario> claseDe(TipoUsuario tipo) {
        return switch (tipo) {
            case APODERADO -> Apoderado.class;
            case DOCENTE -> Docente.class;
            case ESTUDIANTE -> Estudiante.class;
        };
    }

    private static String dniDe(Aceptada a) {
        if (a.apoderado != null) {
            return a.apoderado.dni();
        }
        return a.docente != null ? a.docente.dni() : a.estudiante.dni();
    }

    private static String emailDe(Aceptada a) {
        if (a.apoderado != null) {
            return a.apoderado.email();
        }
        return a.docente != null ? a.docente.email() : a.estudiante.email();
    }

    /** Username = parte local del correo (antes del @). */
    private static String usernameDe(String email) {
        int arroba = email.indexOf('@');
        return arroba < 0 ? email : email.substring(0, arroba);
    }

    private static Set<String> enMinusculas(Collection<String> valores) {
        return valores.stream().map(CargaMasivaUsuariosService::minusculas).collect(Collectors.toCollection(HashSet::new));
    }

    private static String minusculas(String valor) {
        return valor.toLowerCase(Locale.ROOT);
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
