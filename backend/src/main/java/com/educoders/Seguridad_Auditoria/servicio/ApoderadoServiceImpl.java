package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionNotaResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.EvaluacionResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.NotaResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.EvaluacionService;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.NotaService;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.ApoderadoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.ApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CursoEstudianteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.EvaluacionesEstudianteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.EstudianteApoderadoResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarApoderadoRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reglas de negocio de HU-GEMAP-01.
 * Responsabilidad única: alta de apoderados.
 */
@Service
public class ApoderadoServiceImpl implements ApoderadoService {

    private final UsuarioRepository usuarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final AnioAcademicoRepository anioAcademicoRepository;
    private final AsignacionCursoRepository asignacionCursoRepository;
    private final AuditoriaService auditoriaService;
    private final EvaluacionService evaluacionService;
    private final NotaService notaService;

    public ApoderadoServiceImpl(
            UsuarioRepository usuarioRepository,
            ApoderadoRepository apoderadoRepository,
            EstudianteRepository estudianteRepository,
            MatriculaRepository matriculaRepository,
            AnioAcademicoRepository anioAcademicoRepository,
            AsignacionCursoRepository asignacionCursoRepository,
            AuditoriaService auditoriaService,
            EvaluacionService evaluacionService,
            NotaService notaService) {

        this.usuarioRepository = usuarioRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.estudianteRepository = estudianteRepository;
        this.matriculaRepository = matriculaRepository;
        this.anioAcademicoRepository = anioAcademicoRepository;
        this.asignacionCursoRepository = asignacionCursoRepository;
        this.auditoriaService = auditoriaService;
        this.evaluacionService = evaluacionService;
        this.notaService = notaService;
    }

    /**
     * Registra un apoderado y crea su cuenta de usuario.
     */
    @Override
    @Transactional
    public ApoderadoResponse registrarApoderado(RegistrarApoderadoRequest solicitud) {

        validarUnicidad(solicitud.dni(), solicitud.email());

        Apoderado apoderado = new Apoderado();

        apoderado.setNombre(solicitud.nombre());
        apoderado.setApellidoPaterno(solicitud.apellidoPaterno());
        apoderado.setApellidoMaterno(solicitud.apellidoMaterno());
        apoderado.setDni(solicitud.dni());

        // Username es la parte local del email (antes del @)
        apoderado.setUsername(extraerUsernameDeEmail(solicitud.email()));

        apoderado.setEmail(solicitud.email());
        apoderado.setTelefono(solicitud.telefono());

        // La cuenta nace SIN contraseña (passwordHash nulo): el administrador no la conoce ni la define.
        // Decisión de alcance: al registrar no se envía ningún correo. El usuario decide cuándo ingresar y, como no
        // tiene contraseña, la obtiene con el restablecimiento (HU-SAT-03).

        apoderado.setFechaNacimiento(solicitud.fechaNacimiento());
        apoderado.setActivo(true);
        apoderado.setFechaCreacion(LocalDateTime.now());

        Apoderado apoderadoGuardado =
                apoderadoRepository.save(apoderado);
        // HU-SAT-04: el alta y su registro de auditoría se confirman juntos (misma transacción).
        auditoriaService.registrarAuditoria(AuditoriaUsuarios.registro(apoderadoGuardado));

        return convertirARespuesta(apoderadoGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstudianteApoderadoResponse> listarMisEstudiantes(Long anioId) {
        Apoderado apoderado = exigirApoderado();

        return estudianteRepository.findByApoderadoIdOrderByIdAsc(apoderado.getId())
                .stream()
                .map(estudiante -> convertirARespuesta(estudiante, anioId))
                .toList();
    }

    private EstudianteApoderadoResponse convertirARespuesta(Estudiante estudiante, Long anioId) {
        Matricula matricula = null;
        if (anioId != null) {
            matricula = matriculaRepository.buscarPorEstudianteYAnioIdYEstados(
                            estudiante.getId(),
                            anioId,
                            List.of(EstadoMatricula.PENDIENTE, EstadoMatricula.CONFIRMADA))
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        String nombreCompleto = String.join(
                " ",
                estudiante.getNombre(),
                estudiante.getApellidoPaterno(),
                estudiante.getApellidoMaterno() == null ? "" : estudiante.getApellidoMaterno()
        ).trim();

        if (matricula == null) {
            return new EstudianteApoderadoResponse(
                    estudiante.getId(),
                    estudiante.getNombre(),
                    estudiante.getApellidoPaterno(),
                    estudiante.getApellidoMaterno(),
                    nombreCompleto,
                    estudiante.getDni(),
                    estudiante.getCodigo(),
                    null, null, null, null, null, null, null
            );
        }

        return new EstudianteApoderadoResponse(
                estudiante.getId(),
                estudiante.getNombre(),
                estudiante.getApellidoPaterno(),
                estudiante.getApellidoMaterno(),
                nombreCompleto,
                estudiante.getDni(),
                estudiante.getCodigo(),
                matricula.getSeccion().getGrado().getId(),
                matricula.getSeccion().getGrado().getNombre(),
                matricula.getSeccion().getId(),
                matricula.getSeccion().getNombre(),
                matricula.getSeccion().getAnioAcademico().getId(),
                matricula.getSeccion().getAnioAcademico().getAnio(),
                matricula.getEstado()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CursoEstudianteResponse> listarCursosEstudiante(Long estudianteId) {
        Apoderado apoderado = exigirApoderado();

        // Verificar que el estudiante pertenece al apoderado
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Estudiante", estudianteId));

        if (!estudiante.getApoderado().getId().equals(apoderado.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El estudiante no está vinculado al apoderado autenticado.");
        }

        // Obtener el año académico vigente
        LocalDate hoy = LocalDate.now();
        AnioAcademico anioVigente = anioAcademicoRepository
                .findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(hoy, hoy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay un año académico vigente."));

        // Buscar matrícula confirmada del estudiante en ese año
        List<Matricula> matriculas = matriculaRepository.buscarPorEstudianteYAnioIdYEstados(
                estudianteId,
                anioVigente.getId(),
                List.of(EstadoMatricula.CONFIRMADA)
        );

        if (matriculas.isEmpty()) {
            return List.of();
        }

        Matricula matricula = matriculas.get(0);
        Long seccionId = matricula.getSeccion().getId();

        // Buscar asignaciones de curso para esa sección
        List<AsignacionCurso> asignaciones = asignacionCursoRepository.findBySeccionId(seccionId);

        return asignaciones.stream()
                .map(asignacion -> new CursoEstudianteResponse(
                        asignacion.getCurso().getId(),
                        asignacion.getCurso().getNombre(),
                        matricula.getSeccion().getGrado().getNombre(),
                        matricula.getSeccion().getId(),
                        matricula.getSeccion().getNombre(),
                        new CursoEstudianteResponse.AnioAcademicoInfo(
                                anioVigente.getId(),
                                anioVigente.getAnio()
                        )
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluacionesEstudianteResponse listarEvaluacionesEstudianteCurso(Long estudianteId, Long cursoId) {
        Apoderado apoderado = exigirApoderado();

        // Verificar que el estudiante pertenece al apoderado
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Estudiante", estudianteId));

        if (!estudiante.getApoderado().getId().equals(apoderado.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El estudiante no está vinculado al apoderado autenticado.");
        }

        // Obtener evaluaciones del curso
        List<EvaluacionResponse> evaluaciones = evaluacionService.listarEvaluaciones(
                Optional.empty(), Optional.empty(), Optional.of(cursoId)
        );

        // Para cada evaluación, buscar la nota del estudiante
        List<EvaluacionNotaResponse> evaluacionesConNotas = evaluaciones.stream()
                .map(evaluacion -> {
                    List<NotaResponse> notas = notaService.listarNotasPorEvaluacion(evaluacion.idEvaluacion());
                    NotaResponse notaEstudiante = notas.stream()
                            .filter(nota -> nota.idEstudiante().equals(estudianteId))
                            .findFirst()
                            .orElse(null);

                    return new EvaluacionNotaResponse(
                            evaluacion.idEvaluacion(),
                            evaluacion.nombre(),
                            evaluacion.descripcion(),
                            evaluacion.fecha(),
                            evaluacion.nombreBimestre(),
                            evaluacion.nombreCompetencia(),
                            notaEstudiante != null ? notaEstudiante.valorNota() : null
                    );
                })
                .toList();

        String nombreCompletoEstudiante = String.join(
                " ",
                estudiante.getNombre(),
                estudiante.getApellidoPaterno(),
                estudiante.getApellidoMaterno() == null ? "" : estudiante.getApellidoMaterno()
        ).trim();

        return new EvaluacionesEstudianteResponse(
                estudianteId,
                nombreCompletoEstudiante,
                cursoId,
                evaluaciones.isEmpty() ? "" : evaluaciones.get(0).nombreCurso(),
                evaluacionesConNotas
        );
    }

    private Apoderado exigirApoderado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        if (!(usuario instanceof Apoderado apoderado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un apoderado puede consultar sus estudiantes.");
        }
        return apoderado;
    }

    private ApoderadoResponse convertirARespuesta(Apoderado apoderado) {

        String nombreCompleto = String.join(
                " ",
                apoderado.getNombre(),
                apoderado.getApellidoPaterno(),
                apoderado.getApellidoMaterno() == null
                        ? ""
                        : apoderado.getApellidoMaterno()
        ).trim();

        return new ApoderadoResponse(
                apoderado.getId(),
                apoderado.getNombre(),
                apoderado.getApellidoPaterno(),
                apoderado.getApellidoMaterno(),
                nombreCompleto,
                apoderado.getDni(),
                apoderado.getUsername(),
                apoderado.getEmail(),
                apoderado.getTelefono(),
                apoderado.getFechaNacimiento(),
                apoderado.getEdad(),
                apoderado.isActivo(),
                apoderado.getFechaCreacion()
        );
    }

    private void validarUnicidad(String dni, String email) {

        if (usuarioRepository.existsByDniAndTipoUsuario(dni, Apoderado.class)) {
            throw new ConflictoException(
                    "Ya existe un apoderado registrado con el DNI indicado."
            );
        }

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException(
                    "Ya existe un usuario registrado con el correo indicado."
            );
        }

        if (usuarioRepository.existsByUsername(extraerUsernameDeEmail(email))) {
            throw new ConflictoException(
                    "Ya existe un usuario registrado con ese identificador."
            );
        }
    }

    private String extraerUsernameDeEmail(String email) {
        return email.substring(0, email.indexOf('@'));
    }
}