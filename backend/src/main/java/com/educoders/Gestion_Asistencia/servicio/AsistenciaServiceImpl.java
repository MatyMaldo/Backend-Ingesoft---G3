package com.educoders.Gestion_Asistencia.servicio;

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
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
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

    private static final long MINUTOS_TARDANZA_DOCENTE = 10;

    private final SesionClaseRepository sesionRepository;
    private final AsistenciaEstudianteRepository asistenciaEstudianteRepository;
    private final AsistenciaDocenteRepository asistenciaDocenteRepository;
    private final MatriculaRepository matriculaRepository;
    private final EstudianteRepository estudianteRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public AsistenciaServiceImpl(
            SesionClaseRepository sesionRepository,
            AsistenciaEstudianteRepository asistenciaEstudianteRepository,
            AsistenciaDocenteRepository asistenciaDocenteRepository,
            MatriculaRepository matriculaRepository,
            EstudianteRepository estudianteRepository,
            AuditoriaService auditoriaService,
            Clock clock) {
        this.sesionRepository = sesionRepository;
        this.asistenciaEstudianteRepository = asistenciaEstudianteRepository;
        this.asistenciaDocenteRepository = asistenciaDocenteRepository;
        this.matriculaRepository = matriculaRepository;
        this.estudianteRepository = estudianteRepository;
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

        List<AsistenciaEstudiante> guardar = new ArrayList<>();
        for (GuardarAsistenciaEstudiantesRequest.AsistenciaItem item : request.asistencias()) {
            AsistenciaEstudiante asistencia = existentes.get(item.estudianteId());
            if (asistencia == null) {
                asistencia = new AsistenciaEstudiante();
                asistencia.setSesionClase(sesion);
                asistencia.setEstudiante(rosterPorId.get(item.estudianteId()).getEstudiante());
            }
            asistencia.setEstado(item.estado());
            asistencia.setObservaciones(item.observaciones());
            asistencia.setFechaHoraRegistro(ahora);
            guardar.add(asistencia);
        }

        if (!guardar.isEmpty()) {
            asistenciaEstudianteRepository.saveAll(guardar);
            auditar("AsistenciaEstudiante", sesionId,
                    "Registro de asistencia de estudiantes");
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
                sesion.getHoraInicio().plusMinutes(MINUTOS_TARDANZA_DOCENTE))
                ? EstadoAsistencia.TARDANZA
                : EstadoAsistencia.PRESENTE;

        AsistenciaDocente asistencia = new AsistenciaDocente();
        asistencia.setSesionClase(sesion);
        asistencia.setDocente(docente);
        asistencia.setEstado(estado);
        asistencia.setFechaHoraRegistro(ahora);
        AsistenciaDocente guardada = asistenciaDocenteRepository.save(asistencia);

        auditar("AsistenciaDocente", sesionId, "Registro de asistencia del docente");
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

        Page<AsistenciaDocente> registros = asistenciaDocenteRepository
                .buscarHistorial(docente.getId(), desde, hasta, cursoId,
                        PageRequest.of(paginaNormalizada, tamanoNormalizado));
        List<Object[]> cursos = asistenciaDocenteRepository
                .buscarCursosHistorial(docente.getId(), desde, hasta);
        List<Object[]> conteos = asistenciaDocenteRepository
                .contarPorEstado(docente.getId(), desde, hasta, cursoId);
        return construirHistorialDocente(
                registros, cursos, conteos, paginaNormalizada, tamanoNormalizado);
    }

    @Override
    @Transactional(readOnly = true)
    public AsistenciaHistorialResponse historialEstudiante(
            LocalDate desde, LocalDate hasta, Long cursoId, int pagina, int tamano) {
        Estudiante estudiante = exigirEstudiante();
        validarRango(desde, hasta);
        int paginaNormalizada = normalizarPagina(pagina);
        int tamanoNormalizado = normalizarTamano(tamano);

        Page<AsistenciaEstudiante> registros = asistenciaEstudianteRepository
                .buscarHistorial(estudiante.getId(), desde, hasta, cursoId,
                        PageRequest.of(paginaNormalizada, tamanoNormalizado));
        List<Object[]> cursos = asistenciaEstudianteRepository
                .buscarCursosHistorial(estudiante.getId(), desde, hasta);
        List<Object[]> conteos = asistenciaEstudianteRepository
                .contarPorEstado(estudiante.getId(), desde, hasta, cursoId);
        return construirHistorialEstudiante(
                registros, cursos, conteos, paginaNormalizada, tamanoNormalizado);
    }

    @Override
    @Transactional(readOnly = true)
    public AsistenciaHistorialResponse historialApoderado(
            Long estudianteId, LocalDate desde, LocalDate hasta, Long cursoId,
            int pagina, int tamano) {
        Apoderado apoderado = exigirApoderado();
        validarRango(desde, hasta);
        int paginaNormalizada = normalizarPagina(pagina);
        int tamanoNormalizado = normalizarTamano(tamano);

        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> RecursoNoEncontradoException.de(
                        "estudiante", estudianteId));
        if (estudiante.getApoderado() == null
                || !estudiante.getApoderado().getId().equals(apoderado.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "El estudiante no se encuentra a su cargo.");
        }

        Page<AsistenciaEstudiante> registros = asistenciaEstudianteRepository
                .buscarHistorial(estudianteId, desde, hasta, cursoId,
                        PageRequest.of(paginaNormalizada, tamanoNormalizado));
        List<Object[]> cursos = asistenciaEstudianteRepository
                .buscarCursosHistorial(estudianteId, desde, hasta);
        List<Object[]> conteos = asistenciaEstudianteRepository
                .contarPorEstado(estudianteId, desde, hasta, cursoId);
        return construirHistorialEstudiante(
                registros, cursos, conteos, paginaNormalizada, tamanoNormalizado);
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
            int tamano) {
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
                                    null);
                        })
                        .toList();

        ResumenAsistencia resumen = resumir(conteos, registros.getTotalElements());
        return new AsistenciaHistorialResponse(
                cursos, items, pagina, tamano,
                registros.getTotalElements(), registros.getTotalPages(),
                resumen.presentes(), resumen.tardanzas(),
                resumen.faltas(), resumen.justificadas(),
                resumen.porcentajeAsistencia());
    }

    private AsistenciaHistorialResponse construirHistorialEstudiante(
            Page<AsistenciaEstudiante> registros,
            List<Object[]> cursosRaw,
            List<Object[]> conteos,
            int pagina,
            int tamano) {
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
                                    r.getObservaciones());
                        })
                        .toList();

        ResumenAsistencia resumen = resumir(conteos, registros.getTotalElements());
        return new AsistenciaHistorialResponse(
                cursos, items, pagina, tamano,
                registros.getTotalElements(), registros.getTotalPages(),
                resumen.presentes(), resumen.tardanzas(),
                resumen.faltas(), resumen.justificadas(),
                resumen.porcentajeAsistencia());
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

        double porcentaje = totalRegistros == 0
                ? 0.0
                : Math.round((presentes + tardanzas) * 10000.0 / totalRegistros) / 100.0;

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

    private void auditar(String entidad, Long entidadId, String descripcion) {
        try {
            auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                    null,
                    CategoriaAuditoria.ASISTENCIA,
                    TipoAccionAuditoria.ACTUALIZAR,
                    entidad,
                    entidadId,
                    descripcion,
                    List.of()
            ));
        } catch (RuntimeException ignored) {
            // La auditoría no debe impedir el registro de asistencia.
        }
    }
}
