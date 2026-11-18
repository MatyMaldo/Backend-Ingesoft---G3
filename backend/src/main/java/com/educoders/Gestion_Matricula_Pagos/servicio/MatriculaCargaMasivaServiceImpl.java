package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.GradoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ConfirmarCargaMasivaMatriculasRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ConfirmarCargaMasivaMatriculasResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.PreviewCargaMasivaMatriculasResponse;
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleAuditoriaDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MatriculaCargaMasivaServiceImpl implements MatriculaCargaMasivaService {

    private static final String[] ENCABEZADOS = {
            "DNI", "Código", "Año", "Grado", "Sección"
    };

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final List<EstadoMatricula> ESTADOS_ACTIVOS = List.of(
            EstadoMatricula.PENDIENTE,
            EstadoMatricula.CONFIRMADA);

    private final MatriculaRepository matriculaRepository;
    private final PeriodoMatriculaRepository periodoMatriculaRepository;
    private final SeccionRepository seccionRepository;
    private final GradoRepository gradoRepository;
    private final EstudianteRepository estudianteRepository;
    private final AnioAcademicoRepository anioAcademicoRepository;
    private final MatriculaArchivoParser archivoParser;
    private final AuditoriaService auditoriaService;

    public MatriculaCargaMasivaServiceImpl(
            MatriculaRepository matriculaRepository,
            PeriodoMatriculaRepository periodoMatriculaRepository,
            SeccionRepository seccionRepository,
            GradoRepository gradoRepository,
            EstudianteRepository estudianteRepository,
            AnioAcademicoRepository anioAcademicoRepository,
            MatriculaArchivoParser archivoParser,
            AuditoriaService auditoriaService) {
        this.matriculaRepository = matriculaRepository;
        this.periodoMatriculaRepository = periodoMatriculaRepository;
        this.seccionRepository = seccionRepository;
        this.gradoRepository = gradoRepository;
        this.estudianteRepository = estudianteRepository;
        this.anioAcademicoRepository = anioAcademicoRepository;
        this.archivoParser = archivoParser;
        this.auditoriaService = auditoriaService;
    }

    @Override
    public byte[] generarPlantilla(String formato) {
        exigirAdministrador();
        return generarArchivo(List.of(), formato);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportar(Long anioId, String formato) {
        exigirAdministrador();

        if (anioId == null) {
            throw new ReglaNegocioException("El año académico es obligatorio.");
        }

        AnioAcademico anio = anioAcademicoRepository.findById(anioId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Año académico", anioId));

        List<Matricula> matriculas = matriculaRepository
                .buscarPorAnioYEstadosConDetalle(anioId, ESTADOS_ACTIVOS);

        List<String[]> filas = new ArrayList<>(matriculas.size());
        for (Matricula matricula : matriculas) {
            filas.add(new String[]{
                    matricula.getEstudiante().getDni(),
                    matricula.getEstudiante().getCodigo(),
                    String.valueOf(anio.getAnio()),
                    matricula.getSeccion().getGrado().getNombre(),
                    matricula.getSeccion().getNombre()
            });
        }

        return generarArchivo(filas, formato);
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewCargaMasivaMatriculasResponse preview(
            MultipartFile archivo, Long anioAcademicoId) {

        exigirAdministrador();
        validarArchivo(archivo);

        AnioAcademico anio = obtenerAnioVigente(anioAcademicoId);
        List<MatriculaArchivoParser.FilaMatricula> filas = archivoParser.leer(archivo);

        ContextoPrevio contexto = cargarContexto(anio, filas);

        List<PreviewCargaMasivaMatriculasResponse.FilaPreview> resultado = new ArrayList<>();
        int validas = 0;
        int conError = 0;
        int duplicadas = 0;
        int conflictos = 0;

        for (int i = 0; i < filas.size(); i++) {
            ResultadoFila resultadoFila = validarFila(
                    filas.get(i), anio, contexto, nombre(archivo), i + 1);
            resultado.add(resultadoFila.preview());

            switch (resultadoFila.estado()) {
                case VALIDO -> validas++;
                case ERROR -> conError++;
                case DUPLICADO -> duplicadas++;
                case CONFLICTO -> conflictos++;
            }
        }

        List<PreviewCargaMasivaMatriculasResponse.SeccionDisponible> secciones = contexto.seccionesDelAnio
                .stream()
                .sorted(Comparator
                        .comparing((Seccion s) -> s.getGrado().getId())
                        .thenComparing(Seccion::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(s -> {
                    long ocupados = contexto.ocupacionBase.getOrDefault(s.getId(), 0L);
                    long vacantes = Math.max(0L, s.getCapacidadMaxima() - ocupados);
                    return new PreviewCargaMasivaMatriculasResponse.SeccionDisponible(
                            s.getId(),
                            s.getNombre(),
                            s.getGrado().getId(),
                            s.getGrado().getNombre(),
                            anio.getId(),
                            anio.getAnio(),
                            s.getCapacidadMaxima(),
                            ocupados,
                            vacantes);
                })
                .toList();

        return new PreviewCargaMasivaMatriculasResponse(
                new PreviewCargaMasivaMatriculasResponse.Resumen(
                        filas.size(), validas, conError, duplicadas, conflictos),
                resultado,
                secciones);
    }

    @Override
    @Transactional
    public ConfirmarCargaMasivaMatriculasResponse confirmar(
            ConfirmarCargaMasivaMatriculasRequest request) {

        exigirAdministrador();

        if (request == null || request.anioAcademicoId() == null) {
            throw new ReglaNegocioException("El año académico es obligatorio.");
        }
        if (request.filas() == null || request.filas().isEmpty()) {
            throw new ReglaNegocioException("La lista de filas es obligatoria.");
        }

        AnioAcademico anio = obtenerAnioVigente(request.anioAcademicoId());
        ContextoConfirmacion contexto = cargarContextoConfirmacion(anio, request.filas());

        List<ConfirmarCargaMasivaMatriculasResponse.ErrorConfirmacion> errores = new ArrayList<>();
        List<Matricula> aCrear = new ArrayList<>();

        for (ConfirmarCargaMasivaMatriculasRequest.FilaConfirmacion fila : request.filas()) {
            String origen = fila == null || fila.origen() == null || fila.origen().isBlank()
                    ? "fila"
                    : fila.origen();

            if (fila == null) {
                errores.add(new ConfirmarCargaMasivaMatriculasResponse.ErrorConfirmacion(
                        origen, "ERROR", "La fila está vacía."));
                continue;
            }

            try {
                Matricula matricula = validarFilaConfirmacion(fila, anio, contexto);
                aCrear.add(matricula);
            } catch (FilaException ex) {
                errores.add(new ConfirmarCargaMasivaMatriculasResponse.ErrorConfirmacion(
                        origen, "ERROR", ex.getMessage()));
            }
        }

        if (!errores.isEmpty()) {
            return new ConfirmarCargaMasivaMatriculasResponse(0, errores);
        }

        try {
            // Un INSERT de varias filas por lote en lugar de uno por matrícula (Matricula usa IDENTITY).
            matriculaRepository.insertarEnLotes(aCrear);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictoException(
                    "No se pudo completar la carga masiva: existe un registro de matrícula "
                            + "que entra en conflicto con los datos enviados.");
        }

        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                null,
                CategoriaAuditoria.MATRICULA,
                TipoAccionAuditoria.CREAR,
                "Matricula",
                null,
                "Carga masiva de matrículas: " + aCrear.size()
                        + " matrícula(s) creada(s) para el año " + anio.getAnio(),
                List.of(
                        new DetalleAuditoriaDTO(
                                "anioAcademicoId", null, String.valueOf(anio.getId())),
                        new DetalleAuditoriaDTO(
                                "cantidad", null, String.valueOf(aCrear.size()))
                )
        ));

        return new ConfirmarCargaMasivaMatriculasResponse(aCrear.size(), List.of());
    }

    private Matricula validarFilaConfirmacion(
            ConfirmarCargaMasivaMatriculasRequest.FilaConfirmacion fila,
            AnioAcademico anio,
            ContextoConfirmacion contexto) {

        if (fila.estudianteId() == null) {
            throw new FilaException("El estudiante es obligatorio.");
        }
        if (fila.seccionId() == null) {
            throw new FilaException("La sección es obligatoria.");
        }
        if (fila.anioAcademicoId() != null
                && !fila.anioAcademicoId().equals(anio.getId())) {
            throw new FilaException("La fila no pertenece al año académico indicado.");
        }

        Estudiante estudiante = contexto.estudiantesPorId.get(fila.estudianteId());
        if (estudiante == null) {
            throw new FilaException("No existe el estudiante indicado.");
        }
        if (!estudiante.isActivo()) {
            throw new FilaException("El estudiante está inactivo.");
        }

        Seccion seccion = contexto.seccionesPorId.get(fila.seccionId());
        if (seccion == null) {
            throw new FilaException("No existe la sección indicada.");
        }
        if (seccion.getAnioAcademico() == null
                || !seccion.getAnioAcademico().getId().equals(anio.getId())) {
            throw new FilaException("La sección no pertenece al año académico indicado.");
        }
        if (fila.gradoId() != null
                && !fila.gradoId().equals(seccion.getGrado().getId())) {
            throw new FilaException("La sección no pertenece al grado indicado.");
        }

        String referenciaPrevia = contexto.estudiantesVistos.get(estudiante.getId());
        if (referenciaPrevia != null) {
            throw new FilaException(
                    "El estudiante está repetido; ya aparece en " + referenciaPrevia + ".");
        }
        contexto.estudiantesVistos.put(
                estudiante.getId(),
                fila.origen() == null || fila.origen().isBlank()
                        ? "otra fila"
                        : fila.origen());

        for (Matricula existente : contexto.matriculasPorEstudiante
                .getOrDefault(estudiante.getId(), List.of())) {
            if (existente.getEstado() == EstadoMatricula.PENDIENTE
                    || existente.getEstado() == EstadoMatricula.CONFIRMADA) {
                throw new FilaException(
                        "El estudiante ya tiene una matrícula "
                                + existente.getEstado().name()
                                + " en el año " + anio.getAnio() + ".");
            }
        }

        for (Matricula existente : contexto.matriculasPorEstudiante
                .getOrDefault(estudiante.getId(), List.of())) {
            if (existente.getSeccion().getId().equals(seccion.getId())) {
                throw new FilaException(
                        "El estudiante ya tiene un registro de matrícula en la sección destino.");
            }
        }

        long ocupacion = contexto.ocupacionPreview.getOrDefault(seccion.getId(), 0L);
        if (ocupacion >= seccion.getCapacidadMaxima()) {
            throw new FilaException(
                    "La sección '" + seccion.getNombre()
                            + "' del grado '" + seccion.getGrado().getNombre()
                            + "' ya alcanzó su capacidad máxima ("
                            + seccion.getCapacidadMaxima() + ").");
        }
        contexto.ocupacionPreview.put(seccion.getId(), ocupacion + 1);

        return Matricula.builder()
                .fechaAsignacion(LocalDate.now())
                .estado(EstadoMatricula.PENDIENTE)
                .estudiante(estudiante)
                .seccion(seccion)
                .build();
    }

    private ResultadoFila validarFila(
            MatriculaArchivoParser.FilaMatricula fila,
            AnioAcademico anio,
            ContextoPrevio contexto,
            String nombreArchivo,
            int indice) {

        String previewId = "p-" + indice;
        String origen = nombreArchivo + "#" + fila.fila();
        List<String> errores = new ArrayList<>();

        boolean errorDatos = false;
        Estudiante estudiante = null;
        Grado grado = null;
        Seccion seccion = null;

        String dni = limpiar(fila.dni());
        String codigo = limpiar(fila.codigo());

        if (dni.isBlank() && codigo.isBlank()) {
            errores.add("Debe indicar el DNI o el código del estudiante.");
            errorDatos = true;
        }

        Estudiante porDni = dni.isBlank() ? null : contexto.estudiantesPorDni.get(dni);
        Estudiante porCodigo = codigo.isBlank() ? null : contexto.estudiantesPorCodigo.get(codigo);

        if (!dni.isBlank() && porDni == null) {
            errores.add("No existe un estudiante con el DNI '" + dni + "'.");
            errorDatos = true;
        }
        if (!codigo.isBlank() && porCodigo == null) {
            errores.add("No existe un estudiante con el código '" + codigo + "'.");
            errorDatos = true;
        }
        if (porDni != null && porCodigo != null
                && !porDni.getId().equals(porCodigo.getId())) {
            errores.add("El DNI y el código no corresponden al mismo estudiante.");
            errorDatos = true;
        }

        estudiante = porDni != null ? porDni : porCodigo;
        if (estudiante != null && !estudiante.isActivo()) {
            errores.add("El estudiante está inactivo.");
            errorDatos = true;
        }

        if (fila.grado().isBlank()) {
            errores.add("El grado es obligatorio.");
            errorDatos = true;
        } else {
            grado = contexto.gradosPorNombre.get(
                    MatriculaArchivoParser.normalizar(fila.grado()));
            if (grado == null) {
                errores.add("No existe el grado '" + fila.grado() + "'.");
                errorDatos = true;
            }
        }

        if (grado != null) {
            if (fila.seccion().isBlank()) {
                errores.add("La sección es obligatoria.");
                errorDatos = true;
            } else {
                seccion = contexto.seccionesPorGradoYNombre.get(
                        claveSeccion(grado, fila.seccion()));
                if (seccion == null) {
                    errores.add(
                            "No existe la sección '" + fila.seccion()
                                    + "' para el grado '" + grado.getNombre()
                                    + "' en el año " + anio.getAnio() + ".");
                    errorDatos = true;
                }
            }
        }

        boolean duplicado = false;
        boolean conflicto = false;

        if (!errorDatos && estudiante != null && seccion != null) {
            String referenciaPrevia = contexto.estudiantesVistos.get(estudiante.getId());

            if (referenciaPrevia != null) {
                duplicado = true;
                errores.add("El estudiante ya aparece en la fila "
                        + referenciaPrevia + " del archivo.");
            } else {
                contexto.estudiantesVistos.put(
                        estudiante.getId(), String.valueOf(fila.fila()));

                for (Matricula existente : contexto.matriculasPorEstudiante
                        .getOrDefault(estudiante.getId(), List.of())) {
                    if (existente.getEstado() == EstadoMatricula.PENDIENTE
                            || existente.getEstado() == EstadoMatricula.CONFIRMADA) {
                        conflicto = true;
                        errores.add("El estudiante ya tiene una matrícula "
                                + existente.getEstado().name()
                                + " en el año " + anio.getAnio() + ".");
                        break;
                    }
                }

                if (!conflicto) {
                    for (Matricula existente : contexto.matriculasPorEstudiante
                            .getOrDefault(estudiante.getId(), List.of())) {
                        if (existente.getSeccion().getId().equals(seccion.getId())) {
                            conflicto = true;
                            errores.add(
                                    "El estudiante ya tiene un registro de matrícula "
                                            + "en la sección destino (estado "
                                            + existente.getEstado().name() + ").");
                            break;
                        }
                    }
                }

                if (!conflicto) {
                    long ocupacion = contexto.ocupacionPreview
                            .getOrDefault(seccion.getId(), 0L);
                    if (ocupacion >= seccion.getCapacidadMaxima()) {
                        conflicto = true;
                        errores.add("La sección '" + seccion.getNombre()
                                + "' del grado '" + grado.getNombre()
                                + "' ya alcanzó su capacidad máxima ("
                                + seccion.getCapacidadMaxima() + ").");
                    } else {
                        contexto.ocupacionPreview.put(seccion.getId(), ocupacion + 1);
                    }
                }
            }
        }

        Estado estado = errorDatos
                ? Estado.ERROR
                : duplicado
                        ? Estado.DUPLICADO
                        : conflicto ? Estado.CONFLICTO : Estado.VALIDO;

        var preview = new PreviewCargaMasivaMatriculasResponse.FilaPreview(
                previewId,
                origen,
                fila.fila(),
                estudiante != null ? estudiante.getId() : null,
                estudiante != null ? estudiante.getDni() : dni,
                estudiante != null ? estudiante.getCodigo() : codigo,
                nombreCompleto(estudiante),
                anio.getId(),
                grado != null ? grado.getId() : null,
                grado != null ? grado.getNombre() : null,
                seccion != null ? seccion.getId() : null,
                seccion != null ? seccion.getNombre() : null,
                estado.name(),
                errores);

        return new ResultadoFila(preview, estado);
    }

    private ContextoPrevio cargarContexto(
            AnioAcademico anio,
            List<MatriculaArchivoParser.FilaMatricula> filas) {

        Map<String, Grado> gradosPorNombre = new HashMap<>();
        for (Grado grado : gradoRepository.findAll()) {
            gradosPorNombre.put(MatriculaArchivoParser.normalizar(grado.getNombre()), grado);
        }

        List<Seccion> secciones = seccionRepository.findByAnioAcademicoId(anio.getId());
        Map<String, Seccion> seccionesPorGradoYNombre = new HashMap<>();
        for (Seccion seccion : secciones) {
            seccionesPorGradoYNombre.put(
                    claveSeccion(seccion.getGrado(), seccion.getNombre()), seccion);
        }

        Set<String> dnis = new HashSet<>();
        Set<String> codigos = new HashSet<>();
        for (MatriculaArchivoParser.FilaMatricula fila : filas) {
            if (!limpiar(fila.dni()).isBlank()) {
                dnis.add(limpiar(fila.dni()));
            }
            if (!limpiar(fila.codigo()).isBlank()) {
                codigos.add(limpiar(fila.codigo()));
            }
        }

        Map<String, Estudiante> estudiantesPorDni = new HashMap<>();
        if (!dnis.isEmpty()) {
            for (Estudiante estudiante : estudianteRepository.buscarPorDnis(dnis)) {
                estudiantesPorDni.put(estudiante.getDni(), estudiante);
            }
        }

        Map<String, Estudiante> estudiantesPorCodigo = new HashMap<>();
        if (!codigos.isEmpty()) {
            for (Estudiante estudiante : estudianteRepository.buscarPorCodigos(codigos)) {
                estudiantesPorCodigo.put(estudiante.getCodigo(), estudiante);
            }
        }

        Set<Long> estudianteIds = new HashSet<>();
        estudiantesPorDni.values().forEach(e -> estudianteIds.add(e.getId()));
        estudiantesPorCodigo.values().forEach(e -> estudianteIds.add(e.getId()));

        Map<Long, List<Matricula>> matriculasPorEstudiante = new HashMap<>();
        if (!estudianteIds.isEmpty()) {
            for (Matricula matricula : matriculaRepository
                    .buscarPorEstudiantesYAnio(estudianteIds, anio.getId())) {
                matriculasPorEstudiante
                        .computeIfAbsent(matricula.getEstudiante().getId(), k -> new ArrayList<>())
                        .add(matricula);
            }
        }

        Map<Long, Long> ocupacionBase = cargarOcupacion(secciones);

        return new ContextoPrevio(
                secciones,
                gradosPorNombre,
                seccionesPorGradoYNombre,
                estudiantesPorDni,
                estudiantesPorCodigo,
                matriculasPorEstudiante,
                ocupacionBase);
    }

    private ContextoConfirmacion cargarContextoConfirmacion(
            AnioAcademico anio,
            List<ConfirmarCargaMasivaMatriculasRequest.FilaConfirmacion> filas) {

        Set<Long> estudianteIds = new HashSet<>();
        Set<Long> seccionIds = new HashSet<>();
        for (ConfirmarCargaMasivaMatriculasRequest.FilaConfirmacion fila : filas) {
            if (fila == null) {
                continue;
            }
            if (fila.estudianteId() != null) {
                estudianteIds.add(fila.estudianteId());
            }
            if (fila.seccionId() != null) {
                seccionIds.add(fila.seccionId());
            }
        }

        Map<Long, Estudiante> estudiantesPorId = new HashMap<>();
        if (!estudianteIds.isEmpty()) {
            for (Estudiante estudiante : estudianteRepository.findAllById(estudianteIds)) {
                estudiantesPorId.put(estudiante.getId(), estudiante);
            }
        }

        Map<Long, Seccion> seccionesPorId = new HashMap<>();
        if (!seccionIds.isEmpty()) {
            for (Seccion seccion : seccionRepository.findAllById(seccionIds)) {
                seccionesPorId.put(seccion.getId(), seccion);
            }
        }

        Map<Long, List<Matricula>> matriculasPorEstudiante = new HashMap<>();
        if (!estudianteIds.isEmpty()) {
            for (Matricula matricula : matriculaRepository
                    .buscarPorEstudiantesYAnio(estudianteIds, anio.getId())) {
                matriculasPorEstudiante
                        .computeIfAbsent(matricula.getEstudiante().getId(), k -> new ArrayList<>())
                        .add(matricula);
            }
        }

        Map<Long, Long> ocupacionBase = new HashMap<>();
        if (!seccionIds.isEmpty()) {
            for (Object[] fila : matriculaRepository
                    .contarPorSecciones(seccionIds, ESTADOS_ACTIVOS)) {
                ocupacionBase.put(
                        ((Number) fila[0]).longValue(),
                        ((Number) fila[1]).longValue());
            }
        }

        return new ContextoConfirmacion(
                estudiantesPorId, seccionesPorId, matriculasPorEstudiante, ocupacionBase);
    }

    private Map<Long, Long> cargarOcupacion(List<Seccion> secciones) {
        Map<Long, Long> ocupacion = new HashMap<>();
        List<Long> seccionIds = secciones.stream().map(Seccion::getId).toList();
        if (seccionIds.isEmpty()) {
            return ocupacion;
        }
        for (Object[] fila : matriculaRepository.contarPorSecciones(seccionIds, ESTADOS_ACTIVOS)) {
            ocupacion.put(
                    ((Number) fila[0]).longValue(),
                    ((Number) fila[1]).longValue());
        }
        return ocupacion;
    }

    private AnioAcademico obtenerAnioVigente(Long anioId) {
        if (anioId == null) {
            throw new ReglaNegocioException("El año académico es obligatorio.");
        }

        AnioAcademico anio = anioAcademicoRepository.findById(anioId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Año académico", anioId));

        PeriodoMatricula periodo = periodoMatriculaRepository
                .findByAnioAcademicoId(anioId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El período de matrícula no está habilitado para el año académico "
                                + anio.getAnio() + "."));

        if (!periodo.estaVigente(LocalDate.now())) {
            throw new ReglaNegocioException(
                    "El período de matrícula ha finalizado o aún no se encuentra vigente.");
        }

        return anio;
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Debe seleccionar un archivo no vacío.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ReglaNegocioException("El archivo no debe superar los 5 MB.");
        }

        String nombre = nombre(archivo).toLowerCase(Locale.ROOT);
        if (!nombre.endsWith(".csv")
                && !nombre.endsWith(".xlsx")
                && !nombre.endsWith(".xls")) {
            throw new ReglaNegocioException("Formato no soportado. Use CSV o XLSX.");
        }
    }

    private byte[] generarArchivo(List<String[]> filas, String formato) {
        String extension = formato == null || formato.isBlank()
                ? "csv"
                : formato.toLowerCase(Locale.ROOT);

        if ("xlsx".equals(extension) || "xls".equals(extension)) {
            return generarExcel(filas);
        }
        if (!"csv".equals(extension)) {
            throw new ReglaNegocioException("Formato no soportado. Use csv o xlsx.");
        }
        return generarCsv(filas);
    }

    private byte[] generarCsv(List<String[]> filas) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(String.join(";", ENCABEZADOS)).append("\r\n");
        for (String[] fila : filas) {
            for (int i = 0; i < fila.length; i++) {
                if (i > 0) {
                    sb.append(';');
                }
                sb.append(escaparCsv(fila[i]));
            }
            sb.append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escaparCsv(String valor) {
        String texto = valor == null ? "" : valor;
        if (texto.contains(";") || texto.contains("\"")
                || texto.contains("\n") || texto.contains("\r")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    private byte[] generarExcel(List<String[]> filas) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            Sheet hoja = workbook.createSheet("Matriculas");
            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                cabecera.createCell(i).setCellValue(ENCABEZADOS[i]);
            }

            for (int i = 0; i < filas.size(); i++) {
                Row fila = hoja.createRow(i + 1);
                String[] valores = filas.get(i);
                for (int c = 0; c < valores.length; c++) {
                    fila.createCell(c).setCellValue(valores[c] == null ? "" : valores[c]);
                }
            }

            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new ReglaNegocioException("No se pudo generar el archivo Excel.");
        }
    }

    private String claveSeccion(Grado grado, String nombreSeccion) {
        return grado.getId() + "|" + MatriculaArchivoParser.normalizar(nombreSeccion);
    }

    private String nombreCompleto(Estudiante estudiante) {
        if (estudiante == null) {
            return null;
        }
        String nombre = estudiante.getNombre() + " " + estudiante.getApellidoPaterno();
        if (estudiante.getApellidoMaterno() != null
                && !estudiante.getApellidoMaterno().isBlank()) {
            nombre += " " + estudiante.getApellidoMaterno();
        }
        return nombre;
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String nombre(MultipartFile archivo) {
        return archivo.getOriginalFilename() == null
                ? "archivo"
                : archivo.getOriginalFilename();
    }

    private void exigirAdministrador() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        if (!(usuario instanceof Administrador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un administrador puede gestionar matrículas.");
        }
    }

    private enum Estado {
        VALIDO,
        ERROR,
        DUPLICADO,
        CONFLICTO
    }

    private record ResultadoFila(
            PreviewCargaMasivaMatriculasResponse.FilaPreview preview,
            Estado estado
    ) {
    }

    private static class FilaException extends RuntimeException {
        FilaException(String mensaje) {
            super(mensaje);
        }
    }

    private static final class ContextoPrevio {
        private final List<Seccion> seccionesDelAnio;
        private final Map<String, Grado> gradosPorNombre;
        private final Map<String, Seccion> seccionesPorGradoYNombre;
        private final Map<String, Estudiante> estudiantesPorDni;
        private final Map<String, Estudiante> estudiantesPorCodigo;
        private final Map<Long, List<Matricula>> matriculasPorEstudiante;
        private final Map<Long, Long> ocupacionBase;
        private final Map<Long, Long> ocupacionPreview;
        private final Map<Long, String> estudiantesVistos = new HashMap<>();

        private ContextoPrevio(
                List<Seccion> seccionesDelAnio,
                Map<String, Grado> gradosPorNombre,
                Map<String, Seccion> seccionesPorGradoYNombre,
                Map<String, Estudiante> estudiantesPorDni,
                Map<String, Estudiante> estudiantesPorCodigo,
                Map<Long, List<Matricula>> matriculasPorEstudiante,
                Map<Long, Long> ocupacionBase) {
            this.seccionesDelAnio = seccionesDelAnio;
            this.gradosPorNombre = gradosPorNombre;
            this.seccionesPorGradoYNombre = seccionesPorGradoYNombre;
            this.estudiantesPorDni = estudiantesPorDni;
            this.estudiantesPorCodigo = estudiantesPorCodigo;
            this.matriculasPorEstudiante = matriculasPorEstudiante;
            this.ocupacionBase = ocupacionBase;
            this.ocupacionPreview = new HashMap<>(ocupacionBase);
        }
    }

    private static final class ContextoConfirmacion {
        private final Map<Long, Estudiante> estudiantesPorId;
        private final Map<Long, Seccion> seccionesPorId;
        private final Map<Long, List<Matricula>> matriculasPorEstudiante;
        private final Map<Long, Long> ocupacionPreview;
        private final Map<Long, String> estudiantesVistos = new HashMap<>();

        private ContextoConfirmacion(
                Map<Long, Estudiante> estudiantesPorId,
                Map<Long, Seccion> seccionesPorId,
                Map<Long, List<Matricula>> matriculasPorEstudiante,
                Map<Long, Long> ocupacionBase) {
            this.estudiantesPorId = estudiantesPorId;
            this.seccionesPorId = seccionesPorId;
            this.matriculasPorEstudiante = matriculasPorEstudiante;
            this.ocupacionPreview = new HashMap<>(ocupacionBase);
        }
    }
}
