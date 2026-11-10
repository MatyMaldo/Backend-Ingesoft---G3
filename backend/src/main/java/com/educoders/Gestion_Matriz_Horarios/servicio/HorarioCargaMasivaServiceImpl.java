package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AsignacionCurso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Curso;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AsignacionCursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.CursoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.GradoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.VentanaMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Matriz_Horarios.config.HorarioProperties;
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.persistencia.AulaRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ConfirmarCargaMasivaRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ConfirmarCargaMasivaResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ExportarBorradorRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.PreviewCargaMasivaResponse;
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HorarioCargaMasivaServiceImpl implements HorarioCargaMasivaService {

    public static final String[] ENCABEZADOS = {
            "ID Sección", "Grado", "Sección", "ID Curso", "Curso",
            "ID Docente", "DNI Docente", "Docente", "ID Aula", "Aula",
            "Día", "Hora inicio", "Hora fin"
    };

    private static final int MAX_ARCHIVOS = 10;
    private static final int MAX_FILAS = 5000;
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final PeriodoMatriculaRepository periodoRepo;
    private final AnioAcademicoRepository anioRepo;
    private final GradoRepository gradoRepo;
    private final SeccionRepository seccionRepo;
    private final CursoRepository cursoRepo;
    private final DocenteRepository docenteRepo;
    private final AulaRepository aulaRepo;
    private final AsignacionCursoRepository asignacionRepo;
    private final BloqueHorarioRepository bloqueRepo;
    private final HorarioProperties horarioProperties;
    private final AuditoriaService auditoriaService;

    public HorarioCargaMasivaServiceImpl(PeriodoMatriculaRepository periodoRepo,
                                         AnioAcademicoRepository anioRepo,
                                         GradoRepository gradoRepo,
                                         SeccionRepository seccionRepo,
                                         CursoRepository cursoRepo,
                                         DocenteRepository docenteRepo,
                                         AulaRepository aulaRepo,
                                         AsignacionCursoRepository asignacionRepo,
                                         BloqueHorarioRepository bloqueRepo,
                                         HorarioProperties horarioProperties,
                                         AuditoriaService auditoriaService) {
        this.periodoRepo = periodoRepo;
        this.anioRepo = anioRepo;
        this.gradoRepo = gradoRepo;
        this.seccionRepo = seccionRepo;
        this.cursoRepo = cursoRepo;
        this.docenteRepo = docenteRepo;
        this.aulaRepo = aulaRepo;
        this.asignacionRepo = asignacionRepo;
        this.bloqueRepo = bloqueRepo;
        this.horarioProperties = horarioProperties;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewCargaMasivaResponse preview(List<MultipartFile> archivos, Long anioAcademicoId) {
        AnioAcademico anio = validarCabecera(archivos, anioAcademicoId);

        List<PreviewCargaMasivaResponse.ArchivoPreview> resumenArchivos = new ArrayList<>();
        List<Fila> todas = new ArrayList<>();

        for (MultipartFile archivo : archivos) {
            String nombre = nombre(archivo);
            try {
                List<Fila> filas = leerArchivo(archivo, anio);
                todas.addAll(filas);
                resumenArchivos.add(new PreviewCargaMasivaResponse.ArchivoPreview(
                        nombre, filas.size(), 0));
            } catch (Exception ex) {
                resumenArchivos.add(new PreviewCargaMasivaResponse.ArchivoPreview(
                        nombre, 0, 1));
            }
        }

        if (todas.isEmpty()) {
            throw new ReglaNegocioException("Ningún archivo se pudo interpretar.");
        }
        if (todas.size() > MAX_FILAS) {
            throw new ReglaNegocioException("La carga supera el máximo de " + MAX_FILAS + " filas.");
        }

        List<PreviewCargaMasivaResponse.BloquePreview> bloques = new ArrayList<>();
        List<Validado> validos = new ArrayList<>();
        Map<String, Boolean> clavesVistas = new HashMap<>();
        Map<String, Integer> erroresPorArchivo = new HashMap<>();

        int indice = 0;
        for (Fila fila : todas) {
            indice++;
            String previewId = "p-" + indice;
            try {
                Validado validado = resolver(fila, anio);
                String clave = clave(validado);
                PreviewCargaMasivaResponse.BloquePreview bloque = preview(
                        validado, previewId, fila.fila(), "VALIDO", List.of());

                if (clavesVistas.containsKey(clave)) {
                    bloque = preview(validado, previewId, fila.fila(), "DUPLICADO",
                            List.of("Bloque duplicado dentro de la carga."));
                } else if (hayConflicto(validado, validos)
                        || hayConflictoEnBase(validado, anio.getId())) {
                    bloque = preview(validado, previewId, fila.fila(), "CONFLICTO",
                            List.of("El docente, la sección o el aula ya tienen un bloque en ese horario."));
                } else {
                    validos.add(validado);
                }
                clavesVistas.put(clave, true);
                bloques.add(bloque);
                if (!"VALIDO".equals(bloque.estado())) {
                    erroresPorArchivo.merge(fila.archivo(), 1, Integer::sum);
                }
            } catch (FilaException ex) {
                bloques.add(previewError(fila, previewId, ex.getMessage()));
                erroresPorArchivo.merge(fila.archivo(), 1, Integer::sum);
            }
        }

        resumenArchivos = resumenArchivos.stream()
                .map(a -> new PreviewCargaMasivaResponse.ArchivoPreview(
                        a.nombre(), a.filas(), erroresPorArchivo.getOrDefault(a.nombre(), a.errores())))
                .toList();

        int validas = (int) bloques.stream().filter(b -> "VALIDO".equals(b.estado())).count();
        int conError = (int) bloques.stream().filter(b -> "ERROR".equals(b.estado())).count();
        int duplicadas = (int) bloques.stream().filter(b -> "DUPLICADO".equals(b.estado())).count();
        int conflictos = (int) bloques.stream().filter(b -> "CONFLICTO".equals(b.estado())).count();

        return new PreviewCargaMasivaResponse(
                new PreviewCargaMasivaResponse.Resumen(
                        archivos.size(), todas.size(), validas, conError, duplicadas, conflictos),
                resumenArchivos,
                bloques
        );
    }

    @Override
    @Transactional
    public ConfirmarCargaMasivaResponse confirmar(ConfirmarCargaMasivaRequest request) {
        if (request == null || request.anioAcademicoId() == null) {
            throw new ReglaNegocioException("El año académico es obligatorio.");
        }

        AnioAcademico anio = anioRepo.findById(request.anioAcademicoId())
                .orElseThrow(() -> new ReglaNegocioException("Año académico inválido."));
        validarProgramacionAbierta(anio.getId());

        List<ConfirmarCargaMasivaResponse.ErrorConfirmacion> errores = new ArrayList<>();
        List<Validado> validados = new ArrayList<>();
        Set<String> claves = new HashSet<>();

        if (request.bloques() == null || request.bloques().isEmpty()) {
            throw new ReglaNegocioException("La lista de bloques es obligatoria.");
        }

        for (ConfirmarCargaMasivaRequest.BloqueConfirmacion bloque : request.bloques()) {
            String origen = bloque.origen() != null ? bloque.origen() : "bloque";
            try {
                AsignacionCurso asignacion = asignacionRepo.findById(bloque.asignacionCursoId())
                        .orElseThrow(() -> new FilaException("No existe la asignación de curso."));
                Seccion seccion = asignacion.getSeccion();
                if (seccion.getAnioAcademico() == null
                        || !anio.getId().equals(seccion.getAnioAcademico().getId())) {
                    throw new FilaException("La asignación no pertenece al año académico indicado.");
                }
                Aula aula = aulaRepo.findById(bloque.aulaId())
                        .orElseThrow(() -> new FilaException("No existe el aula."));
                if (bloque.diaSemana() == null) {
                    throw new FilaException("El día es obligatorio.");
                }
                validarHorario(bloque.horaInicio(), bloque.horaFin());

                Validado validado = new Validado(
                        asignacion, seccion, asignacion.getCurso(), asignacion.getDocente(),
                        aula, bloque.diaSemana(), bloque.horaInicio(), bloque.horaFin(), origen);
                String clave = clave(validado);
                if (!claves.add(clave)) {
                    throw new FilaException("Bloque duplicado dentro de la solicitud.");
                }
                if (hayConflicto(validado, validados) || hayConflictoEnBase(validado, anio.getId())) {
                    throw new FilaException("El docente, la sección o el aula ya tienen un bloque en ese horario.");
                }
                validados.add(validado);
            } catch (FilaException ex) {
                errores.add(new ConfirmarCargaMasivaResponse.ErrorConfirmacion(
                        origen, "ERROR", ex.getMessage()));
            }
        }

        if (!errores.isEmpty()) {
            return new ConfirmarCargaMasivaResponse(0, errores);
        }

        List<BloqueHorario> bloques = validados.stream().map(v -> {
            BloqueHorario bloque = new BloqueHorario();
            bloque.setAsignacionCurso(v.asignacion());
            bloque.setAula(v.aula());
            bloque.setDiaSemana(v.diaSemana());
            bloque.setHoraInicio(v.horaInicio());
            bloque.setHoraFin(v.horaFin());
            return bloque;
        }).toList();

        bloqueRepo.saveAll(bloques);

        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                null,
                CategoriaAuditoria.HORARIOS,
                TipoAccionAuditoria.CREAR,
                "BloqueHorario",
                null,
                "Carga masiva confirmada: " + bloques.size() + " bloque(s) creado(s)",
                List.of()
        ));

        return new ConfirmarCargaMasivaResponse(bloques.size(), List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportar(Long anioId, Long gradoId, Long seccionId, Long docenteId, String formato) {
        List<BloqueHorario> bloques = bloqueRepo.buscarParaExportar(
                anioId, gradoId, seccionId, docenteId);
        List<String[]> filas = bloques.stream().map(this::filaExportacion).toList();
        return generarArchivo(filas, formato);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarBorrador(ExportarBorradorRequest request) {
        if (request == null || request.anioAcademicoId() == null
                || request.bloques() == null || request.bloques().isEmpty()) {
            throw new ReglaNegocioException("No hay bloques para exportar.");
        }

        AnioAcademico anio = anioRepo.findById(request.anioAcademicoId())
                .orElseThrow(() -> new ReglaNegocioException("Año académico inválido."));

        List<String[]> filas = new ArrayList<>();
        for (ConfirmarCargaMasivaRequest.BloqueConfirmacion bloque : request.bloques()) {
            AsignacionCurso asignacion = asignacionRepo.findById(bloque.asignacionCursoId())
                    .orElseThrow(() -> new ReglaNegocioException("No existe la asignación de curso."));
            if (asignacion.getSeccion().getAnioAcademico() == null
                    || !anio.getId().equals(asignacion.getSeccion().getAnioAcademico().getId())) {
                throw new ReglaNegocioException(
                        "Una asignación no pertenece al año académico indicado.");
            }
            Aula aula = aulaRepo.findById(bloque.aulaId())
                    .orElseThrow(() -> new ReglaNegocioException("No existe el aula."));
            if (bloque.diaSemana() == null || bloque.horaInicio() == null || bloque.horaFin() == null) {
                throw new ReglaNegocioException(
                        "Cada bloque debe indicar día, hora de inicio y hora de fin.");
            }

            filas.add(new String[]{
                    String.valueOf(asignacion.getSeccion().getId()),
                    asignacion.getSeccion().getGrado().getNombre(),
                    asignacion.getSeccion().getNombre(),
                    String.valueOf(asignacion.getCurso().getId()),
                    asignacion.getCurso().getNombre(),
                    String.valueOf(asignacion.getDocente().getId()),
                    asignacion.getDocente().getDni(),
                    asignacion.getDocente().getNombre() + " "
                            + asignacion.getDocente().getApellidoPaterno(),
                    String.valueOf(aula.getId()),
                    aula.getNombre(),
                    bloque.diaSemana().name(),
                    bloque.horaInicio().format(FORMATO_HORA),
                    bloque.horaFin().format(FORMATO_HORA)
            });
        }

        return generarArchivo(filas, request.formato());
    }

    @Override
    public byte[] generarPlantilla(String formato) {
        List<String[]> filas = List.<String[]>of(new String[]{
                "1", "1er Grado de Secundaria", "A", "1", "Matemática",
                "2", "1234567", "Juan Perez", "101", "Aula 101",
                "LUNES", "08:00", "09:00"
        });
        return generarArchivo(filas, formato);
    }

    // ------------------------------------------------------------------
    // Lectura de archivos
    // ------------------------------------------------------------------

    private AnioAcademico validarCabecera(List<MultipartFile> archivos, Long anioAcademicoId) {
        if (archivos == null || archivos.isEmpty()) {
            throw new ReglaNegocioException("No se enviaron archivos.");
        }
        if (archivos.size() > MAX_ARCHIVOS) {
            throw new ReglaNegocioException("Se permiten como máximo " + MAX_ARCHIVOS + " archivos.");
        }
        if (anioAcademicoId == null) {
            throw new ReglaNegocioException("El año académico es obligatorio.");
        }

        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                throw new ReglaNegocioException("Uno de los archivos está vacío.");
            }
            if (archivo.getSize() > MAX_BYTES) {
                throw new ReglaNegocioException("El archivo " + nombre(archivo) + " supera los 10 MB.");
            }
            String nombre = nombre(archivo).toLowerCase(Locale.ROOT);
            if (!nombre.endsWith(".csv") && !nombre.endsWith(".xlsx") && !nombre.endsWith(".xls")) {
                throw new ReglaNegocioException("Formato no soportado: " + nombre(archivo));
            }
        }

        AnioAcademico anio = anioRepo.findById(anioAcademicoId)
                .orElseThrow(() -> new ReglaNegocioException("Año académico inválido."));
        validarProgramacionAbierta(anio.getId());
        return anio;
    }

    private void validarProgramacionAbierta(Long anioId) {
        PeriodoMatricula periodo = periodoRepo.findByAnioAcademicoId(anioId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No existe un periodo de matrícula para el año académico."));
        if (!VentanaMatricula.crudBloquesHorarioAbierta(periodo, LocalDate.now())) {
            throw new ReglaNegocioException(
                    "La programación de horarios está cerrada: solo se habilita durante el periodo de matrícula.");
        }
    }

    private List<Fila> leerArchivo(MultipartFile archivo, AnioAcademico anio) throws IOException {
        String nombre = nombre(archivo);
        String extension = nombre.toLowerCase(Locale.ROOT);
        if (extension.endsWith(".csv")) {
            return leerCsv(archivo, nombre);
        }
        return leerExcel(archivo, nombre);
    }

    private List<Fila> leerCsv(MultipartFile archivo, String nombre) throws IOException {
        String contenido = new String(archivo.getBytes(), StandardCharsets.UTF_8);
        if (contenido.startsWith("\uFEFF")) {
            contenido = contenido.substring(1);
        }
        String[] lineas = contenido.split("\\r?\\n");
        if (lineas.length == 0) {
            throw new ReglaNegocioException("El archivo CSV está vacío.");
        }
        char separador = detectarSeparador(lineas[0]);
        int ultima = lineas.length;
        while (ultima > 0 && lineas[ultima - 1].isBlank()) {
            ultima--;
        }
        if (ultima < 1) {
            throw new ReglaNegocioException("El archivo CSV está vacío.");
        }

        Map<String, Integer> columnas = columnas(lineas[0].split(PatternQuote.of(separador), -1));
        List<Fila> filas = new ArrayList<>();
        for (int i = 1; i < ultima; i++) {
            if (lineas[i].isBlank()) continue;
            String[] valores = lineas[i].split(PatternQuote.of(separador), -1);
            filas.add(crearFila(nombre, i + 1, valores, columnas));
        }
        return filas;
    }

    private List<Fila> leerExcel(MultipartFile archivo, String nombre) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);
            int headerIndex = -1;
            for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && !filaVacia(row, formatter)) {
                    headerIndex = i;
                    break;
                }
            }
            if (headerIndex < 0) {
                throw new ReglaNegocioException("El archivo Excel está vacío.");
            }

            Row header = sheet.getRow(headerIndex);
            List<String> encabezados = new ArrayList<>();
            for (int c = 0; c < Math.max(header.getLastCellNum(), 0); c++) {
                encabezados.add(formatter.formatCellValue(header.getCell(c)));
            }
            Map<String, Integer> columnas = columnas(encabezados.toArray(String[]::new));

            List<Fila> filas = new ArrayList<>();
            for (int i = headerIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || filaVacia(row, formatter)) continue;
                String[] valores = new String[encabezados.size()];
                for (int c = 0; c < valores.length; c++) {
                    Cell cell = row.getCell(c);
                    valores[c] = formatter.formatCellValue(cell);
                }
                filas.add(crearFila(nombre, i + 1, valores, columnas));
            }
            return filas;
        }
    }

    private boolean filaVacia(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) return false;
        }
        return true;
    }

    private char detectarSeparador(String encabezado) {
        long puntoYComa = encabezado.chars().filter(c -> c == ';').count();
        long coma = encabezado.chars().filter(c -> c == ',').count();
        if (puntoYComa == 0 && coma == 0) {
            throw new ReglaNegocioException("No se pudo detectar el separador del CSV.");
        }
        return puntoYComa >= coma ? ';' : ',';
    }

    private Map<String, Integer> columnas(String[] encabezados) {
        Map<String, Integer> columnas = new HashMap<>();
        for (int i = 0; i < encabezados.length; i++) {
            columnas.put(normalizar(encabezados[i]), i);
        }
        boolean formatoNuevo = columnas.containsKey("id seccion");
        boolean formatoViejo = columnas.containsKey("grado")
                && columnas.containsKey("seccion")
                && columnas.containsKey("curso")
                && columnas.containsKey("dni docente")
                && columnas.containsKey("aula");

        if (!formatoNuevo && !formatoViejo) {
            throw new ReglaNegocioException("Encabezados inválidos: use el formato de 8 o 13 columnas.");
        }
        for (String requerida : List.of("dia", "hora inicio", "hora fin")) {
            if (!columnas.containsKey(requerida)) {
                throw new ReglaNegocioException("Falta la columna obligatoria: " + requerida);
            }
        }
        if (formatoNuevo) {
            for (String requerida : List.of("id seccion", "id curso", "id aula")) {
                if (!columnas.containsKey(requerida)) {
                    throw new ReglaNegocioException("Falta la columna obligatoria: " + requerida);
                }
            }
        }
        return columnas;
    }

    private Fila crearFila(String archivo, int numero, String[] valores, Map<String, Integer> columnas) {
        return new Fila(
                archivo,
                numero,
                valor(valores, columnas, "id seccion"),
                valor(valores, columnas, "grado"),
                valor(valores, columnas, "seccion"),
                valor(valores, columnas, "id curso"),
                valor(valores, columnas, "curso"),
                valor(valores, columnas, "id docente"),
                valor(valores, columnas, "dni docente"),
                valor(valores, columnas, "docente"),
                valor(valores, columnas, "id aula"),
                valor(valores, columnas, "aula"),
                valor(valores, columnas, "dia"),
                valor(valores, columnas, "hora inicio"),
                valor(valores, columnas, "hora fin")
        );
    }

    private String valor(String[] valores, Map<String, Integer> columnas, String clave) {
        Integer indice = columnas.get(clave);
        if (indice == null || indice >= valores.length) return "";
        return valores[indice] == null ? "" : valores[indice].trim();
    }

    // ------------------------------------------------------------------
    // Resolución y validación
    // ------------------------------------------------------------------

    private Validado resolver(Fila fila, AnioAcademico anio) {
        try {
            Seccion seccion = resolverSeccion(fila, anio);
            Curso curso = resolverCurso(fila);
            Docente docente = resolverDocente(fila);
            Aula aula = resolverAula(fila);
            DiaSemana dia = parsearDia(fila.dia());
            LocalTime inicio = parsearHora(fila.horaInicio(), "hora de inicio");
            LocalTime fin = parsearHora(fila.horaFin(), "hora de fin");
            validarHorario(inicio, fin);

            AsignacionCurso asignacion = asignacionRepo
                    .findByCursoIdAndSeccionId(curso.getId(), seccion.getId())
                    .orElseThrow(() -> new FilaException(
                            "No existe una asignación curso-docente-sección para esa sección."));
            if (!asignacion.getDocente().getId().equals(docente.getId())) {
                throw new FilaException("El docente no coincide con la asignación del curso en la sección.");
            }

            return new Validado(asignacion, seccion, curso, docente, aula, dia, inicio, fin, fila.archivo());
        } catch (FilaException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FilaException(ex.getMessage() != null ? ex.getMessage() : "Fila inválida.");
        }
    }

    private Seccion resolverSeccion(Fila fila, AnioAcademico anio) {
        Long id = parseLong(fila.idSeccion(), "ID Sección");
        if (id != null) {
            Seccion seccion = seccionRepo.findById(id)
                    .orElseThrow(() -> new FilaException("No existe la sección con ID " + id + "."));
            if (seccion.getAnioAcademico() == null
                    || !anio.getId().equals(seccion.getAnioAcademico().getId())) {
                throw new FilaException("La sección no pertenece al año académico indicado.");
            }
            return seccion;
        }

        String gradoNombre = fila.grado();
        String seccionNombre = fila.seccion();
        if (gradoNombre.isBlank() || seccionNombre.isBlank()) {
            throw new FilaException("Debe indicar ID Sección o Grado/Sección.");
        }
        Grado grado = gradoRepo.findByNombre(gradoNombre)
                .orElseThrow(() -> new FilaException("No existe el grado '" + gradoNombre + "'."));
        return seccionRepo.findByGradoIdAndAnioAcademicoIdAndNombre(
                        grado.getId(), anio.getId(), seccionNombre)
                .orElseThrow(() -> new FilaException(
                        "No existe la sección '" + seccionNombre + "' para el grado indicado."));
    }

    private Curso resolverCurso(Fila fila) {
        Long id = parseLong(fila.idCurso(), "ID Curso");
        if (id != null) {
            return cursoRepo.findById(id)
                    .orElseThrow(() -> new FilaException("No existe el curso con ID " + id + "."));
        }
        if (fila.curso().isBlank()) throw new FilaException("Debe indicar ID Curso o Curso.");
        return cursoRepo.findByNombre(fila.curso())
                .orElseThrow(() -> new FilaException("No existe el curso '" + fila.curso() + "'."));
    }

    private Docente resolverDocente(Fila fila) {
        Long id = parseLong(fila.idDocente(), "ID Docente");
        if (id != null) {
            Docente docente = docenteRepo.findById(id)
                    .orElseThrow(() -> new FilaException("No existe el docente con ID " + id + "."));
            if (!docente.isActivo()) throw new FilaException("El docente no está activo.");
            return docente;
        }
        if (fila.dni().isBlank()) throw new FilaException("Debe indicar ID Docente o DNI Docente.");
        Docente docente = docenteRepo.findByDni(fila.dni())
                .orElseThrow(() -> new FilaException("No existe un docente con DNI '" + fila.dni() + "'."));
        if (!docente.isActivo()) throw new FilaException("El docente no está activo.");
        return docente;
    }

    private Aula resolverAula(Fila fila) {
        Long id = parseLong(fila.idAula(), "ID Aula");
        if (id != null) {
            return aulaRepo.findById(id)
                    .orElseThrow(() -> new FilaException("No existe el aula con ID " + id + "."));
        }
        if (fila.aula().isBlank()) throw new FilaException("Debe indicar ID Aula o Aula.");
        return aulaRepo.findByNombre(fila.aula())
                .orElseThrow(() -> new FilaException("No existe el aula '" + fila.aula() + "'."));
    }

    private void validarHorario(LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null) {
            throw new FilaException("La hora de inicio y fin son obligatorias.");
        }
        if (!fin.isAfter(inicio)) {
            throw new FilaException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        LocalTime jornadaInicio = parsearConfig(horarioProperties.getJornadaInicio());
        LocalTime jornadaFin = parsearConfig(horarioProperties.getJornadaFin());
        LocalTime recesoInicio = parsearConfig(horarioProperties.getRecesoInicio());
        LocalTime recesoFin = parsearConfig(horarioProperties.getRecesoFin());

        if (inicio.isBefore(jornadaInicio) || fin.isAfter(jornadaFin)) {
            throw new FilaException("El bloque está fuera de la jornada permitida.");
        }
        if (inicio.isBefore(recesoFin) && fin.isAfter(recesoInicio)) {
            throw new FilaException("El bloque no puede cruzar el receso.");
        }
    }

    private LocalTime parsearConfig(String valor) {
        try {
            return LocalTime.parse(valor, FORMATO_HORA);
        } catch (RuntimeException e) {
            throw new ReglaNegocioException("Configuración de horario inválida.");
        }
    }

    private DiaSemana parsearDia(String valor) {
        String normalizado = normalizar(valor).toUpperCase(Locale.ROOT);
        return switch (normalizado) {
            case "LUNES" -> DiaSemana.LUNES;
            case "MARTES" -> DiaSemana.MARTES;
            case "MIERCOLES" -> DiaSemana.MIERCOLES;
            case "JUEVES" -> DiaSemana.JUEVES;
            case "VIERNES" -> DiaSemana.VIERNES;
            default -> throw new FilaException("Día inválido: '" + valor + "'.");
        };
    }

    private LocalTime parsearHora(String valor, String etiqueta) {
        for (String patron : List.of("HH:mm", "H:mm", "HH:mm:ss")) {
            try {
                return LocalTime.parse(valor, DateTimeFormatter.ofPattern(patron));
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new FilaException("La " + etiqueta + " debe tener formato HH:mm.");
    }

    private Long parseLong(String valor, String campo) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new FilaException("El campo " + campo + " debe ser numérico.");
        }
    }

    // ------------------------------------------------------------------
    // Conflictos
    // ------------------------------------------------------------------

    private boolean hayConflicto(Validado nuevo, List<Validado> previos) {
        for (Validado actual : previos) {
            if (actual.diaSemana() != nuevo.diaSemana()
                    || !solapan(actual, nuevo)) continue;
            if (actual.seccion().getId().equals(nuevo.seccion().getId())
                    || actual.docente().getId().equals(nuevo.docente().getId())
                    || actual.aula().getId().equals(nuevo.aula().getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean hayConflictoEnBase(Validado v, Long anioId) {
        return !bloqueRepo.findConflictoDocente(
                v.diaSemana(), v.horaInicio(), v.horaFin(), anioId, null, v.docente().getId()).isEmpty()
                || !bloqueRepo.findConflictoSeccion(
                v.diaSemana(), v.horaInicio(), v.horaFin(), anioId, null, v.seccion().getId()).isEmpty()
                || !bloqueRepo.findConflictoAula(
                v.diaSemana(), v.horaInicio(), v.horaFin(), anioId, null, v.aula().getId()).isEmpty();
    }

    private boolean solapan(Validado a, Validado b) {
        return a.horaInicio().isBefore(b.horaFin()) && b.horaInicio().isBefore(a.horaFin());
    }

    private String clave(Validado v) {
        return String.join("|",
                String.valueOf(v.seccion().getId()), String.valueOf(v.curso().getId()),
                String.valueOf(v.docente().getId()), String.valueOf(v.aula().getId()),
                String.valueOf(v.diaSemana()), String.valueOf(v.horaInicio()), String.valueOf(v.horaFin()));
    }

    // ------------------------------------------------------------------
    // Respuesta preview
    // ------------------------------------------------------------------

    private PreviewCargaMasivaResponse.BloquePreview preview(Validado v, String previewId, int fila,
                                                             String estado, List<String> errores) {
        return new PreviewCargaMasivaResponse.BloquePreview(
                previewId, v.origen(), fila, v.asignacion().getId(),
                v.seccion().getId(), v.seccion().getGrado().getNombre(), v.seccion().getNombre(),
                v.curso().getId(), v.curso().getNombre(),
                v.docente().getId(), v.docente().getNombre() + " " + v.docente().getApellidoPaterno(),
                v.aula().getId(), v.aula().getNombre(),
                v.diaSemana(), v.horaInicio(), v.horaFin(), estado, errores);
    }

    private PreviewCargaMasivaResponse.BloquePreview previewError(Fila fila, String previewId, String mensaje) {
        return new PreviewCargaMasivaResponse.BloquePreview(
                previewId, fila.archivo(), fila.fila(), null, null, fila.grado(), fila.seccion(),
                null, fila.curso(), null, fila.docente(), null, fila.aula(),
                null, null, null, "ERROR", List.of(mensaje));
    }

    // ------------------------------------------------------------------
    // Exportación / plantilla
    // ------------------------------------------------------------------

    private String[] filaExportacion(BloqueHorario bloque) {
        var asignacion = bloque.getAsignacionCurso();
        var seccion = asignacion.getSeccion();
        var grado = seccion.getGrado();
        var curso = asignacion.getCurso();
        var docente = asignacion.getDocente();
        return new String[]{
                String.valueOf(seccion.getId()), grado.getNombre(), seccion.getNombre(),
                String.valueOf(curso.getId()), curso.getNombre(),
                String.valueOf(docente.getId()), docente.getDni(),
                docente.getNombre() + " " + docente.getApellidoPaterno(),
                String.valueOf(bloque.getAula().getId()), bloque.getAula().getNombre(),
                bloque.getDiaSemana().name(), bloque.getHoraInicio().format(FORMATO_HORA),
                bloque.getHoraFin().format(FORMATO_HORA)
        };
    }

    private byte[] generarArchivo(List<String[]> filas, String formato) {
        String extension = formato == null ? "csv" : formato.toLowerCase(Locale.ROOT);
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
            sb.append(String.join(";", fila)).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generarExcel(List<String[]> filas) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Horarios");
            Row header = sheet.createRow(0);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                header.createCell(i).setCellValue(ENCABEZADOS[i]);
            }
            for (int i = 0; i < filas.size(); i++) {
                Row row = sheet.createRow(i + 1);
                String[] fila = filas.get(i);
                for (int c = 0; c < fila.length; c++) {
                    row.createCell(c).setCellValue(fila[c] == null ? "" : fila[c]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo generar el archivo Excel.");
        }
    }

    private String nombre(MultipartFile archivo) {
        return archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename();
    }

    private String normalizar(String valor) {
        String sinAcentos = Normalizer.normalize(valor == null ? "" : valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT);
    }

    private record Fila(
            String archivo, int fila,
            String idSeccion, String grado, String seccion,
            String idCurso, String curso,
            String idDocente, String dni, String docente,
            String idAula, String aula,
            String dia, String horaInicio, String horaFin
    ) {
    }

    private record Validado(
            AsignacionCurso asignacion, Seccion seccion, Curso curso, Docente docente,
            Aula aula, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin,
            String origen
    ) {
    }

    private static class FilaException extends RuntimeException {
        FilaException(String mensaje) {
            super(mensaje);
        }
    }

    private static final class PatternQuote {
        private PatternQuote() {
        }

        static String of(char c) {
            return java.util.regex.Pattern.quote(String.valueOf(c));
        }
    }
}
