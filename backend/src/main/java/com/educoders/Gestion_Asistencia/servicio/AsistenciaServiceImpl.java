package com.educoders.Gestion_Asistencia.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Asistencia.dominio.AsistenciaDocente;
import com.educoders.Gestion_Asistencia.dominio.AsistenciaEstudiante;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaDocenteRepository;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaEstudianteRepository;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaDocenteResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaEstudianteDetalleResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaHistorialResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.AsistenciaSesionDetalleResponse;
import com.educoders.Gestion_Asistencia.presentacion.dto.GuardarAsistenciaEstudiantesRequest;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Gestion_Matriz_Horarios.persistencia.SesionClaseRepository;
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleAuditoriaDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AsistenciaServiceImpl implements AsistenciaService {

    @Value("${app.asistencia.tolerancia-docente-minutos:10}")
    private long minutosTardanzaDocente = 10;

    private static final String SIN_REGISTRO = "Sin registro";
    private static final String SIN_OBSERVACIONES = "Sin observaciones";

    private final SesionClaseRepository sesionRepository;
    private final AsistenciaEstudianteRepository asistenciaEstudianteRepository;
    private final AsistenciaDocenteRepository asistenciaDocenteRepository;
    private final MatriculaRepository matriculaRepository;
    private final EstudianteRepository estudianteRepository;
    private final AnioAcademicoRepository anioRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public AsistenciaServiceImpl(
            SesionClaseRepository sesionRepository,
            AsistenciaEstudianteRepository asistenciaEstudianteRepository,
            AsistenciaDocenteRepository asistenciaDocenteRepository,
            MatriculaRepository matriculaRepository,
            EstudianteRepository estudianteRepository,
            AnioAcademicoRepository anioRepository,
            AuditoriaService auditoriaService,
            Clock clock) {
        this.sesionRepository = sesionRepository;
        this.asistenciaEstudianteRepository = asistenciaEstudianteRepository;
        this.asistenciaDocenteRepository = asistenciaDocenteRepository;
        this.matriculaRepository = matriculaRepository;
        this.estudianteRepository = estudianteRepository;
        this.anioRepository = anioRepository;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AsistenciaSesionDetalleResponse obtenerDetalle(Long sesionId) {
        Docente docente = exigirDocente();
        SesionClase sesion = obtenerSesion(sesionId);
        validarDocenteDueno(sesion, docente);
        return construirDetalle(sesion, LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public AsistenciaSesionDetalleResponse guardarAsistenciaEstudiantes(
            Long sesionId,
            GuardarAsistenciaEstudiantesRequest request) {
        Docente docente = exigirDocente();
        SesionClase sesion = obtenerSesion(sesionId);
        validarDocenteDueno(sesion, docente);

        LocalDateTime ahora = LocalDateTime.now(clock);
        String motivo = motivoNoRegistrable(sesion, ahora);
        if (motivo != null) {
            throw new ReglaNegocioException(motivo);
        }
        if (request == null || request.asistencias() == null) {
            throw new ReglaNegocioException("La lista de asistencias es obligatoria.");
        }

        List<Matricula> roster = obtenerRoster(sesion);
        Map<Long, Matricula> rosterPorId = new HashMap<>();
        for (Matricula matricula : roster) {
            rosterPorId.put(matricula.getEstudiante().getId(), matricula);
        }

        Set<Long> enviados = new HashSet<>();
        for (GuardarAsistenciaEstudiantesRequest.AsistenciaItem item : request.asistencias()) {
            if (item == null || item.estudianteId() == null || item.estado() == null) {
                throw new ReglaNegocioException(
                        "Cada asistencia debe indicar estudiante y estado.");
            }
            if (!enviados.add(item.estudianteId())) {
                throw new ReglaNegocioException(
                        "El estudiante " + item.estudianteId() + " está repetido.");
            }
            if (!rosterPorId.containsKey(item.estudianteId())) {
                throw new ReglaNegocioException(
                        "El estudiante " + item.estudianteId()
                                + " no pertenece a la sección de la sesión.");
            }
        }
        if (!enviados.equals(rosterPorId.keySet())) {
            throw new ReglaNegocioException(
                    "Debe registrar exactamente una vez a cada estudiante confirmado.");
        }

        Map<Long, AsistenciaEstudiante> existentes = new HashMap<>();
        for (AsistenciaEstudiante asistencia :
                asistenciaEstudianteRepository.findBySesionClaseId(sesionId)) {
            existentes.put(asistencia.getEstudiante().getId(), asistencia);
        }

        boolean primerRegistro = existentes.isEmpty();
        List<AsistenciaEstudiante> guardar = new ArrayList<>();
        List<DetalleAuditoriaDTO> detalles = new ArrayList<>();
        for (GuardarAsistenciaEstudiantesRequest.AsistenciaItem item : request.asistencias()) {
            AsistenciaEstudiante asistencia = existentes.get(item.estudianteId());
            Estudiante estudiante = rosterPorId.get(item.estudianteId()).getEstudiante();
            String etiqueta = nombreCompleto(estudiante) + " (" + estudiante.getCodigo() + ")";
            String estadoAnterior = asistencia != null ? asistencia.getEstado().name() : SIN_REGISTRO;
            String observacionesAnteriores = asistencia != null
                    ? textoObservaciones(asistencia.getObservaciones())
                    : SIN_REGISTRO;
            boolean existia = asistencia != null;
            if (asistencia == null) {
                asistencia = new AsistenciaEstudiante();
                asistencia.setSesionClase(sesion);
                asistencia.setEstudiante(estudiante);
            }
            if (!estadoAnterior.equals(item.estado().name())) {
                detalles.add(new DetalleAuditoriaDTO(
                        "Asistencia de " + etiqueta, estadoAnterior, item.estado().name()));
            }
            String observacionesNuevas = textoObservaciones(item.observaciones());
            boolean observacionesCambiaron = existia
                    ? !observacionesAnteriores.equals(observacionesNuevas)
                    : !SIN_OBSERVACIONES.equals(observacionesNuevas);
            if (observacionesCambiaron) {
                detalles.add(new DetalleAuditoriaDTO(
                        "Observaciones de " + etiqueta, observacionesAnteriores, observacionesNuevas));
            }
            asistencia.setEstado(item.estado());
            asistencia.setObservaciones(item.observaciones());
            asistencia.setFechaHoraRegistro(ahora);
            guardar.add(asistencia);
        }

        if (!guardar.isEmpty()) {
            asistenciaEstudianteRepository.saveAll(guardar);
            if (!detalles.isEmpty()) {
                auditar(primerRegistro ? TipoAccionAuditoria.CREAR : TipoAccionAuditoria.ACTUALIZAR,
                        "AsistenciaEstudiante", sesionId,
                        primerRegistro
                                ? "Registro de asistencia de estudiantes"
                                : "Modificación de asistencia de estudiantes",
                        detalles);
            }
        }

        return construirDetalle(sesion, LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public AsistenciaDocenteResponse registrarAsistenciaDocente(Long sesionId) {
        Docente docente = exigirDocente();
        SesionClase sesion = obtenerSesion(sesionId);
        validarDocenteDueno(sesion, docente);

        AsistenciaDocente existente = asistenciaDocenteRepository
                .findBySesionClaseId(sesionId)
                .orElse(null);
        if (existente != null) {
            return mapearDocente(existente);
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        String motivo = motivoNoRegistrable(sesion, ahora);
        if (motivo != null) {
            throw new ReglaNegocioException(motivo);
        }

        EstadoAsistencia estado = ahora.isAfter(
                sesion.getHoraInicio().plusMinutes(minutosTardanzaDocente))
                ? EstadoAsistencia.TARDANZA
                : EstadoAsistencia.PRESENTE;

        AsistenciaDocente asistencia = new AsistenciaDocente();
        asistencia.setSesionClase(sesion);
        asistencia.setDocente(docente);
        asistencia.setEstado(estado);
        asistencia.setFechaHoraRegistro(ahora);
        AsistenciaDocente guardada = asistenciaDocenteRepository.save(asistencia);

        auditar(TipoAccionAuditoria.CREAR, "AsistenciaDocente", sesionId,
                "Registro de asistencia del docente",
                List.of(new DetalleAuditoriaDTO(
                        "Asistencia del docente", SIN_REGISTRO, estado.name())));
        return mapearDocente(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public AsistenciaHistorialResponse historialDocente(
            LocalDate desde, LocalDate hasta, Long cursoId, int pagina, int tamano) {
        Docente docente = exigirDocente();
        validarRango(desde, hasta);
        int paginaNormalizada = normalizarPagina(pagina);
        int tamanoNormalizado = normalizarTamano(tamano);

        AnioAcademico anio = anioDeConsulta();
        if (anio == null) {
            return historialVacio(null, paginaNormalizada, tamanoNormalizado);
        }
        LocalDate[] rango = rangoEnAnio(anio, desde, hasta);
        if (rango == null) {
            return historialVacio(anio, paginaNormalizada, tamanoNormalizado);
        }

        Page<AsistenciaDocente> registros = asistenciaDocenteRepository
                .buscarHistorial(docente.getId(), rango[0], rango[1], cursoId,
                        PageRequest.of(paginaNormalizada, tamanoNormalizado));
        List<Object[]> cursos = asistenciaDocenteRepository
                .buscarCursosHistorial(docente.getId(), rango[0], rango[1]);
        List<Object[]> conteos = asistenciaDocenteRepository
                .contarPorEstado(docente.getId(), rango[0], rango[1], cursoId);
        return construirHistorialDocente(
                registros, cursos, conteos, paginaNormalizada, tamanoNormalizado, anio);
    }

    @Override
    @Transactional(readOnly = true)
    public AsistenciaHistorialResponse historialEstudiante(
            LocalDate desde, LocalDate hasta, Long cursoId, int pagina, int tamano) {
        Estudiante estudiante = exigirEstudiante();
        validarRango(desde, hasta);
        return historialDeEstudiante(estudiante.getId(), desde, hasta, cursoId, pagina, tamano);
    }

    @Override
    @Transactional(readOnly = true)
    public AsistenciaHistorialResponse historialApoderado(
            Long estudianteId, LocalDate desde, LocalDate hasta, Long cursoId,
            int pagina, int tamano) {
        Apoderado apoderado = exigirApoderado();
        validarRango(desde, hasta);

        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> RecursoNoEncontradoException.de(
                        "estudiante", estudianteId));
        if (estudiante.getApoderado() == null
                || !estudiante.getApoderado().getId().equals(apoderado.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "El estudiante no se encuentra a su cargo.");
        }
        return historialDeEstudiante(estudianteId, desde, hasta, cursoId, pagina, tamano);
    }

    /**
     * Historial de un estudiante limitado al año académico de consulta (HU-GEMAP-11: el más reciente que ya inició),
     * y solo si el estudiante tiene matrícula Confirmada en ese año. Los rangos pedidos se recortan a ese año.
     */
    private AsistenciaHistorialResponse historialDeEstudiante(
            Long estudianteId, LocalDate desde, LocalDate hasta, Long cursoId,
            int pagina, int tamano) {
        int paginaNormalizada = normalizarPagina(pagina);
        int tamanoNormalizado = normalizarTamano(tamano);

        AnioAcademico anio = anioDeConsulta();
        if (anio == null) {
            return historialVacio(null, paginaNormalizada, tamanoNormalizado);
        }
        boolean matriculado = matriculaRepository
                .existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoIn(
                        estudianteId, anio.getId(), List.of(EstadoMatricula.CONFIRMADA));
        if (!matriculado) {
            return historialVacio(anio, paginaNormalizada, tamanoNormalizado);
        }

        LocalDate[] rango = rangoEnAnio(anio, desde, hasta);
        if (rango == null) {
            return historialVacio(anio, paginaNormalizada, tamanoNormalizado);
        }
        LocalDate desdeEfectivo = rango[0];
        LocalDate hastaEfectivo = rango[1];

        Page<AsistenciaEstudiante> registros = asistenciaEstudianteRepository
                .buscarHistorial(estudianteId, desdeEfectivo, hastaEfectivo, cursoId,
                        PageRequest.of(paginaNormalizada, tamanoNormalizado));
        List<Object[]> cursos = asistenciaEstudianteRepository
                .buscarCursosHistorial(estudianteId, desdeEfectivo, hastaEfectivo);
        List<Object[]> conteos = asistenciaEstudianteRepository
                .contarPorEstado(estudianteId, desdeEfectivo, hastaEfectivo, cursoId);
        return construirHistorialEstudiante(
                registros, cursos, conteos, paginaNormalizada, tamanoNormalizado, anio);
    }

    /** Año académico de consulta (HU-GEMAP-11): el más reciente que ya inició; {@code null} si ninguno inició. */
    private AnioAcademico anioDeConsulta() {
        return anioRepository
                .findFirstByFechaInicioLessThanEqualOrderByFechaInicioDescIdDesc(LocalDate.now(clock))
                .orElse(null);
    }

    /** Recorta el rango pedido al año; devuelve {@code null} si el rango queda fuera del año. */
    private LocalDate[] rangoEnAnio(AnioAcademico anio, LocalDate desde, LocalDate hasta) {
        LocalDate desdeEfectivo = desde == null || desde.isBefore(anio.getFechaInicio())
                ? anio.getFechaInicio() : desde;
        LocalDate hastaEfectivo = hasta == null || hasta.isAfter(anio.getFechaFin())
                ? anio.getFechaFin() : hasta;
        return desdeEfectivo.isAfter(hastaEfectivo) ? null : new LocalDate[]{desdeEfectivo, hastaEfectivo};
    }

    private AsistenciaHistorialResponse historialVacio(AnioAcademico anio, int pagina, int tamano) {
        return new AsistenciaHistorialResponse(
                List.of(), List.of(), pagina, tamano, 0, 0, 0, 0, 0, 0, 0.0, mapearAnio(anio));
    }

    private AsistenciaHistorialResponse.AnioItem mapearAnio(AnioAcademico anio) {
        return anio == null
                ? null
                : new AsistenciaHistorialResponse.AnioItem(
                        anio.getId(), anio.getAnio(), anio.getFechaInicio(), anio.getFechaFin());
    }

    private AsistenciaSesionDetalleResponse construirDetalle(
            SesionClase sesion, LocalDateTime ahora) {
        var asignacion = sesion.getBloqueHorario().getAsignacionCurso();
        var seccion = asignacion.getSeccion();
        var docenteSesion = asignacion.getDocente();

        AsistenciaDocente asistenciaDocente = asistenciaDocenteRepository
                .findBySesionClaseId(sesion.getId())
                .orElse(null);

        Map<Long, AsistenciaEstudiante> asistenciasPorEstudiante = new HashMap<>();
        for (AsistenciaEstudiante asistencia :
                asistenciaEstudianteRepository.findBySesionClaseId(sesion.getId())) {
            asistenciasPorEstudiante.put(asistencia.getEstudiante().getId(), asistencia);
        }

        List<AsistenciaEstudianteDetalleResponse> estudiantes = new ArrayList<>();
        for (Matricula matricula : obtenerRoster(sesion)) {
            Estudiante estudiante = matricula.getEstudiante();
            AsistenciaEstudiante asistencia =
                    asistenciasPorEstudiante.get(estudiante.getId());
            estudiantes.add(new AsistenciaEstudianteDetalleResponse(
                    estudiante.getId(),
                    estudiante.getCodigo(),
                    nombreCompleto(estudiante),
                    asistencia != null ? asistencia.getEstado() : null,
                    asistencia != null ? asistencia.getObservaciones() : null,
                    asistencia != null ? asistencia.getFechaHoraRegistro() : null
            ));
        }

        String motivo = motivoNoRegistrable(sesion, ahora);
        boolean ventanaAbierta = motivo == null;
        boolean puedeDocente = ventanaAbierta && asistenciaDocente == null;
        String motivoDocente = motivo;
        if (motivo == null && asistenciaDocente != null) {
            motivoDocente = "La asistencia docente ya fue registrada.";
        }

        return new AsistenciaSesionDetalleResponse(
                sesion.getId(),
                asignacion.getCurso().getId(),
                asignacion.getCurso().getNombre(),
                seccion.getId(),
                seccion.getNombre(),
                seccion.getGrado().getNombre(),
                docenteSesion.getId(),
                nombreCompleto(docenteSesion),
                sesion.getFecha(),
                sesion.getHoraInicio().toLocalTime(),
                sesion.getHoraFin().toLocalTime(),
                sesion.getEstado(),
                asistenciaDocente != null ? mapearDocente(asistenciaDocente) : null,
                estudiantes,
                puedeDocente,
                ventanaAbierta,
                motivoDocente,
                motivo
        );
    }

    private List<Matricula> obtenerRoster(SesionClase sesion) {
        Long seccionId = sesion.getBloqueHorario()
                .getAsignacionCurso().getSeccion().getId();
        return matriculaRepository.buscarPorSeccionYEstadoConEstudiante(
                seccionId, EstadoMatricula.CONFIRMADA);
    }

    private AsistenciaDocenteResponse mapearDocente(AsistenciaDocente asistencia) {
        return new AsistenciaDocenteResponse(
                asistencia.getEstado(), asistencia.getFechaHoraRegistro());
    }

    private AsistenciaHistorialResponse construirHistorialDocente(
            Page<AsistenciaDocente> registros,
            List<Object[]> cursosRaw,
            List<Object[]> conteos,
            int pagina,
            int tamano,
            AnioAcademico anio) {
        List<AsistenciaHistorialResponse.CursoItem> cursos = mapearCursos(cursosRaw);
        List<AsistenciaHistorialResponse.RegistroItem> items =
                registros.getContent().stream()
                        .map(r -> {
                            var sesion = r.getSesionClase();
                            var asignacion = sesion.getBloqueHorario().getAsignacionCurso();
                            return new AsistenciaHistorialResponse.RegistroItem(
                                    r.getId(),
                                    sesion.getFecha(),
                                    asignacion.getCurso().getId(),
                                    asignacion.getCurso().getNombre(),
                                    asignacion.getSeccion().getNombre(),
                                    asignacion.getSeccion().getGrado().getNombre(),
                                    sesion.getHoraInicio().toLocalTime(),
                                    sesion.getHoraFin().toLocalTime(),
                                    r.getEstado(),
                                    null,
                                    r.getFechaHoraRegistro());
                        })
                        .toList();

        ResumenAsistencia resumen = resumir(conteos, registros.getTotalElements());
        return new AsistenciaHistorialResponse(
                cursos, items, pagina, tamano,
                registros.getTotalElements(), registros.getTotalPages(),
                resumen.presentes(), resumen.tardanzas(),
                resumen.faltas(), resumen.justificadas(),
                resumen.porcentajeAsistencia(), mapearAnio(anio));
    }

    private AsistenciaHistorialResponse construirHistorialEstudiante(
            Page<AsistenciaEstudiante> registros,
            List<Object[]> cursosRaw,
            List<Object[]> conteos,
            int pagina,
            int tamano,
            AnioAcademico anio) {
        List<AsistenciaHistorialResponse.CursoItem> cursos = mapearCursos(cursosRaw);
        List<AsistenciaHistorialResponse.RegistroItem> items =
                registros.getContent().stream()
                        .map(r -> {
                            var sesion = r.getSesionClase();
                            var asignacion = sesion.getBloqueHorario().getAsignacionCurso();
                            return new AsistenciaHistorialResponse.RegistroItem(
                                    r.getId(),
                                    sesion.getFecha(),
                                    asignacion.getCurso().getId(),
                                    asignacion.getCurso().getNombre(),
                                    asignacion.getSeccion().getNombre(),
                                    asignacion.getSeccion().getGrado().getNombre(),
                                    sesion.getHoraInicio().toLocalTime(),
                                    sesion.getHoraFin().toLocalTime(),
                                    r.getEstado(),
                                    r.getObservaciones(),
                                    null);
                        })
                        .toList();

        ResumenAsistencia resumen = resumir(conteos, registros.getTotalElements());
        return new AsistenciaHistorialResponse(
                cursos, items, pagina, tamano,
                registros.getTotalElements(), registros.getTotalPages(),
                resumen.presentes(), resumen.tardanzas(),
                resumen.faltas(), resumen.justificadas(),
                resumen.porcentajeAsistencia(), mapearAnio(anio));
    }

    private ResumenAsistencia resumir(List<Object[]> conteos, long totalRegistros) {
        long presentes = 0;
        long tardanzas = 0;
        long faltas = 0;
        long justificadas = 0;

        for (Object[] row : conteos) {
            EstadoAsistencia estado = (EstadoAsistencia) row[0];
            long cantidad = ((Number) row[1]).longValue();
            switch (estado) {
                case PRESENTE -> presentes = cantidad;
                case TARDANZA -> tardanzas = cantidad;
                case FALTA -> faltas = cantidad;
                case JUSTIFICADA -> justificadas = cantidad;
            }
        }

        // Justificada no cuenta como asistencia ni como falta: sale del cálculo.
        long base = totalRegistros - justificadas;
        double porcentaje = base <= 0
                ? 0.0
                : Math.round((presentes + tardanzas) * 10000.0 / base) / 100.0;

        return new ResumenAsistencia(
                presentes, tardanzas, faltas, justificadas, porcentaje);
    }

    private record ResumenAsistencia(
            long presentes,
            long tardanzas,
            long faltas,
            long justificadas,
            double porcentajeAsistencia) {
    }

    private List<AsistenciaHistorialResponse.CursoItem> mapearCursos(
            List<Object[]> cursosRaw) {
        List<AsistenciaHistorialResponse.CursoItem> cursos = new ArrayList<>();
        for (Object[] row : cursosRaw) {
            cursos.add(new AsistenciaHistorialResponse.CursoItem(
                    ((Number) row[0]).longValue(), (String) row[1]));
        }
        return cursos;
    }

    private int normalizarPagina(int pagina) {
        return Math.max(pagina, 0);
    }

    private int normalizarTamano(int tamano) {
        if (tamano <= 0) {
            return 10;
        }
        return Math.min(tamano, 100);
    }

    private String motivoNoRegistrable(SesionClase sesion, LocalDateTime ahora) {
        if (sesion.getEstado() == EstadoSesion.OMITIDA) {
            return "La sesión está omitida.";
        }
        if (sesion.getEstado() == EstadoSesion.FINALIZADA) {
            return "La sesión ya finalizó.";
        }
        if (ahora.isBefore(sesion.getHoraInicio())) {
            return "La sesión aún no ha comenzado.";
        }
        if (ahora.isAfter(sesion.getHoraFin())) {
            return "La sesión ya terminó.";
        }
        return null;
    }

    private SesionClase obtenerSesion(Long sesionId) {
        return sesionRepository.findById(sesionId)
                .orElseThrow(() -> RecursoNoEncontradoException.de(
                        "sesión de clase", sesionId));
    }

    private void validarDocenteDueno(SesionClase sesion, Docente docente) {
        Long docenteId = sesion.getBloqueHorario()
                .getAsignacionCurso().getDocente().getId();
        if (!docenteId.equals(docente.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el docente de la sesión puede gestionar su asistencia.");
        }
    }

    private Docente exigirDocente() {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (!(usuario instanceof Docente docente)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo un docente puede realizar esta acción.");
        }
        return docente;
    }

    private Estudiante exigirEstudiante() {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (!(usuario instanceof Estudiante estudiante)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo un estudiante puede consultar esta asistencia.");
        }
        return estudiante;
    }

    private Apoderado exigirApoderado() {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (!(usuario instanceof Apoderado apoderado)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo un apoderado puede consultar esta asistencia.");
        }
        return apoderado;
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return usuario;
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException(
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() + " " + usuario.getApellidoPaterno();
        if (usuario.getApellidoMaterno() != null && !usuario.getApellidoMaterno().isBlank()) {
            nombre += " " + usuario.getApellidoMaterno();
        }
        return nombre;
    }

    /**
     * Registra la auditoría dentro de la misma transacción: si falla, no queda asistencia sin su rastro de auditoría.
     */
    private void auditar(TipoAccionAuditoria accion, String entidad, Long entidadId,
                         String descripcion, List<DetalleAuditoriaDTO> detalles) {
        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                null,
                CategoriaAuditoria.ASISTENCIA,
                accion,
                entidad,
                entidadId,
                descripcion,
                detalles
        ));
    }

    private String textoObservaciones(String observaciones) {
        return observaciones == null || observaciones.isBlank() ? SIN_OBSERVACIONES : observaciones.trim();
    }
}
