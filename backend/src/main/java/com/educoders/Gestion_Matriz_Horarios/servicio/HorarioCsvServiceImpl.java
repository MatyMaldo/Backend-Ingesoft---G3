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
import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.persistencia.AulaRepository;
import com.educoders.Gestion_Matriz_Horarios.persistencia.BloqueHorarioRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.CargaMasivaResponse;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.ErrorFila;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.DiaSemana;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleAuditoriaDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class HorarioCsvServiceImpl implements HorarioCsvService {

    public static final String SEPARADOR = ";";

    public static final String[] ENCABEZADOS = {
            "Grado", "Sección", "Curso", "DNI Docente", "Aula", "Día", "Hora inicio", "Hora fin"
    };

    public static final String FILA_EJEMPLO =
            "EJEMPLO;A;Matemática;1234567;101;Lunes;08:00;09:00";

    private static final int MAX_FILAS = 1000;
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
    private final AuditoriaService auditoriaService;

    public HorarioCsvServiceImpl(PeriodoMatriculaRepository periodoRepo,
                                 AnioAcademicoRepository anioRepo,
                                 GradoRepository gradoRepo,
                                 SeccionRepository seccionRepo,
                                 CursoRepository cursoRepo,
                                 DocenteRepository docenteRepo,
                                 AulaRepository aulaRepo,
                                 AsignacionCursoRepository asignacionRepo,
                                 BloqueHorarioRepository bloqueRepo,
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
        this.auditoriaService = auditoriaService;
    }

    // ------------------------------------------------------------------
    // HU-GEMIH-08: plantilla
    // ------------------------------------------------------------------

    @Override
    public byte[] generarPlantilla() {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(String.join(SEPARADOR, ENCABEZADOS)).append("\r\n");
        sb.append(FILA_EJEMPLO).append("\r\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // HU-GEMIH-09: carga masiva
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public CargaMasivaResponse cargarMasivo(MultipartFile archivo, Long anioAcademicoId) {
        validarArchivo(archivo);

        String contenido = leerContenido(archivo);
        String[] lineas = contenido.split("\\r?\\n");
        int ultima = lineas.length;
        while (ultima > 0 && lineas[ultima - 1].isBlank()) {
            ultima--;
        }
        if (ultima < 1) {
            throw new ReglaNegocioException("El archivo CSV está vacío.");
        }

        char separador = detectarSeparador(lineas[0]);
        validarEncabezados(lineas[0], separador);

        int filasDatos = 0;
        for (int i = 1; i < ultima; i++) {
            if (!lineas[i].isBlank()) {
                filasDatos++;
            }
        }
        if (filasDatos > MAX_FILAS) {
            throw new ReglaNegocioException(
                    "El archivo supera el máximo de " + MAX_FILAS + " filas de datos.");
        }

        AnioAcademico anio = resolverAnio(anioAcademicoId);
        validarProgramacionAbierta(anio.getId());

        CargaMasivaResponse respuesta = new CargaMasivaResponse();
        respuesta.setFilasProcesadas(filasDatos);

        List<FilaPlanificada> planificadas = new ArrayList<>();
        Map<Long, List<Intervalo>> porDocente = new HashMap<>();
        Map<Long, List<Intervalo>> porSeccion = new HashMap<>();
        Map<Long, List<Intervalo>> porAula = new HashMap<>();
        Map<String, Long> docentePorCursoSeccion = new HashMap<>();

        for (int i = 1; i < ultima; i++) {
            String linea = lineas[i];
            if (linea.isBlank()) {
                continue;
            }
            int numeroFila = i + 1; // 1-based, incluyendo el encabezado
            try {
                FilaPlanificada fila = parsearFila(linea, separador, anio,
                        porDocente, porSeccion, porAula, docentePorCursoSeccion);
                planificadas.add(fila);
                registrarIntervalos(fila, porDocente, porSeccion, porAula);
            } catch (ErrorFilaException ex) {
                respuesta.getErrores().add(new ErrorFila(numeroFila, ex.getMessage()));
            }
        }

        if (!respuesta.getErrores().isEmpty()) {
            respuesta.setBloquesCreados(0);
            return respuesta;
        }

        int creados = persistir(planificadas);
        respuesta.setBloquesCreados(creados);

        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                null,
                CategoriaAuditoria.HORARIOS,
                TipoAccionAuditoria.CREAR,
                "BloqueHorario",
                null,
                "Carga masiva de horarios: " + creados + " bloque(s) creado(s)",
                List.<DetalleAuditoriaDTO>of()
        ));

        return respuesta;
    }

    // ------------------------------------------------------------------
    // Validaciones generales
    // ------------------------------------------------------------------

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("El archivo CSV es obligatorio.");
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase().endsWith(".csv")) {
            throw new ReglaNegocioException("Solo se acepta un archivo con extensión .csv.");
        }
    }

    private String leerContenido(MultipartFile archivo) {
        try {
            String contenido = new String(archivo.getBytes(), StandardCharsets.UTF_8);
            return contenido.startsWith("\uFEFF") ? contenido.substring(1) : contenido;
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer el archivo: " + e.getMessage());
        }
    }

    private char detectarSeparador(String encabezado) {
        long puntoYComa = encabezado.chars().filter(c -> c == ';').count();
        long coma = encabezado.chars().filter(c -> c == ',').count();
        if (puntoYComa == 0 && coma == 0) {
            throw new ReglaNegocioException(
                    "No se pudo detectar el separador del CSV (use ';' o ',').");
        }
        return puntoYComa >= coma ? ';' : ',';
    }

    private void validarEncabezados(String encabezado, char separador) {
        String[] columnas = encabezado.split(Pattern.quote(String.valueOf(separador)), -1);
        if (columnas.length != ENCABEZADOS.length) {
            throw new ReglaNegocioException(
                    "El CSV debe tener " + ENCABEZADOS.length + " columnas: "
                            + String.join(separador + "", ENCABEZADOS));
        }
        for (int i = 0; i < ENCABEZADOS.length; i++) {
            if (!normalizar(ENCABEZADOS[i]).equals(normalizar(columnas[i]))) {
                throw new ReglaNegocioException(
                        "Encabezado inválido en la columna " + (i + 1) + ": se esperaba '"
                                + ENCABEZADOS[i] + "' y se encontró '" + columnas[i].trim() + "'.");
            }
        }
    }

    private AnioAcademico resolverAnio(Long anioAcademicoId) {
        if (anioAcademicoId != null) {
            return anioRepo.findById(anioAcademicoId)
                    .orElseThrow(() -> new ReglaNegocioException(
                            "No existe el año académico con id " + anioAcademicoId + "."));
        }
        List<AnioAcademico> futuros = anioRepo
                .findByFechaInicioGreaterThanOrderByFechaInicioAsc(LocalDate.now());
        if (futuros.isEmpty()) {
            throw new ReglaNegocioException(
                    "No se pudo inferir el año académico en preparación. Envíe 'anioAcademicoId'.");
        }
        return futuros.get(0);
    }

    private void validarProgramacionAbierta(Long anioAcademicoId) {
        PeriodoMatricula periodo = periodoRepo.findByAnioAcademicoId(anioAcademicoId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No existe un periodo de matrícula registrado para el año académico indicado."));
        if (LocalDate.now().isAfter(periodo.getFechaFin())) {
            throw new ReglaNegocioException(
                    "La programación de horarios está cerrada: solo se habilita mientras el año académico está en preparación.");
        }
    }

    // ------------------------------------------------------------------
    // Parseo y validación de filas
    // ------------------------------------------------------------------

    private FilaPlanificada parsearFila(String linea, char separador, AnioAcademico anio,
                                        Map<Long, List<Intervalo>> porDocente,
                                        Map<Long, List<Intervalo>> porSeccion,
                                        Map<Long, List<Intervalo>> porAula,
                                        Map<String, Long> docentePorCursoSeccion) {
        String[] columnas = linea.split(Pattern.quote(String.valueOf(separador)), -1);
        if (columnas.length != ENCABEZADOS.length) {
            throw new ErrorFilaException(
                    "Se esperaban " + ENCABEZADOS.length + " columnas y se encontraron " + columnas.length + ".");
        }

        String gradoNombre = columnas[0].trim();
        String seccionNombre = columnas[1].trim();
        String cursoNombre = columnas[2].trim();
        String dni = columnas[3].trim();
        String aulaNombre = columnas[4].trim();
        String diaTexto = columnas[5].trim();
        String horaInicioTexto = columnas[6].trim();
        String horaFinTexto = columnas[7].trim();

        if ("EJEMPLO".equalsIgnoreCase(gradoNombre)) {
            throw new ErrorFilaException(
                    "Elimine la fila de ejemplo (EJEMPLO) antes de cargar el archivo.");
        }

        Grado grado = gradoRepo.findByNombre(gradoNombre)
                .orElseThrow(() -> new ErrorFilaException("No existe el grado '" + gradoNombre + "'."));

        Seccion seccion = seccionRepo
                .findByGradoIdAndAnioAcademicoIdAndNombre(grado.getId(), anio.getId(), seccionNombre)
                .orElseThrow(() -> new ErrorFilaException(
                        "No existe la sección '" + seccionNombre + "' para el grado '"
                                + gradoNombre + "' en el año " + anio.getAnio() + "."));

        Curso curso = cursoRepo.findByNombre(cursoNombre)
                .orElseThrow(() -> new ErrorFilaException("No existe el curso '" + cursoNombre + "'."));

        Docente docente = docenteRepo.findByDni(dni)
                .orElseThrow(() -> new ErrorFilaException(
                        "No existe un docente con DNI '" + dni + "'."));
        if (!docente.isActivo()) {
            throw new ErrorFilaException("El docente con DNI '" + dni + "' no está activo.");
        }

        Aula aula = aulaRepo.findByNombre(aulaNombre)
                .orElseThrow(() -> new ErrorFilaException("No existe el aula '" + aulaNombre + "'."));

        DiaSemana dia = parsearDia(diaTexto);
        LocalTime inicio = parsearHora(horaInicioTexto, "hora de inicio");
        LocalTime fin = parsearHora(horaFinTexto, "hora de fin");
        if (!fin.isAfter(inicio)) {
            throw new ErrorFilaException("La hora de fin debe ser posterior a la hora de inicio.");
        }

        validarDocenteDeCursoSeccion(curso, seccion, docente, docentePorCursoSeccion);

        Intervalo intervalo = new Intervalo(dia, inicio, fin);
        if (hayConflicto(porDocente.get(docente.getId()), intervalo)) {
            throw new ErrorFilaException(
                    "El docente ya tiene otro bloque en el mismo horario dentro del archivo.");
        }
        if (hayConflicto(porSeccion.get(seccion.getId()), intervalo)) {
            throw new ErrorFilaException(
                    "La sección ya tiene otro bloque en el mismo horario dentro del archivo.");
        }
        if (hayConflicto(porAula.get(aula.getId()), intervalo)) {
            throw new ErrorFilaException(
                    "El aula ya está asignada en el mismo horario dentro del archivo.");
        }

        if (!bloqueRepo.findConflictoDocente(dia, inicio, fin, anio.getId(), null, docente.getId()).isEmpty()) {
            throw new ErrorFilaException(
                    "No se puede asignar al docente seleccionado en 2 horarios simultáneos.");
        }
        if (!bloqueRepo.findConflictoSeccion(dia, inicio, fin, anio.getId(), null, seccion.getId()).isEmpty()) {
            throw new ErrorFilaException(
                    "No se puede asignar 2 cursos en el mismo bloque de horario.");
        }
        if (!bloqueRepo.findConflictoAula(dia, inicio, fin, anio.getId(), null, aula.getId()).isEmpty()) {
            throw new ErrorFilaException(
                    "El aula seleccionada ya está asignada en ese bloque de horario.");
        }

        return new FilaPlanificada(curso, seccion, docente, aula, dia, inicio, fin);
    }

    private void validarDocenteDeCursoSeccion(Curso curso, Seccion seccion, Docente docente,
                                              Map<String, Long> docentePorCursoSeccion) {
        String clave = curso.getId() + ":" + seccion.getId();
        Optional<AsignacionCurso> existente =
                asignacionRepo.findByCursoIdAndSeccionId(curso.getId(), seccion.getId());

        if (existente.isPresent()) {
            if (!existente.get().getDocente().getId().equals(docente.getId())) {
                throw new ErrorFilaException(
                        "El curso ya está asignado a otro docente en esta sección.");
            }
            return;
        }

        Long docentePrevio = docentePorCursoSeccion.get(clave);
        if (docentePrevio != null && !docentePrevio.equals(docente.getId())) {
            throw new ErrorFilaException(
                    "El curso ya está asignado a otro docente en esta sección dentro del archivo.");
        }
        docentePorCursoSeccion.put(clave, docente.getId());
    }

    private void registrarIntervalos(FilaPlanificada fila,
                                     Map<Long, List<Intervalo>> porDocente,
                                     Map<Long, List<Intervalo>> porSeccion,
                                     Map<Long, List<Intervalo>> porAula) {
        Intervalo intervalo = new Intervalo(fila.dia, fila.inicio, fila.fin);
        porDocente.computeIfAbsent(fila.docente.getId(), k -> new ArrayList<>()).add(intervalo);
        porSeccion.computeIfAbsent(fila.seccion.getId(), k -> new ArrayList<>()).add(intervalo);
        porAula.computeIfAbsent(fila.aula.getId(), k -> new ArrayList<>()).add(intervalo);
    }

    private boolean hayConflicto(List<Intervalo> existentes, Intervalo nuevo) {
        if (existentes == null) {
            return false;
        }
        for (Intervalo actual : existentes) {
            if (seSuperpone(actual, nuevo)) {
                return true;
            }
        }
        return false;
    }

    private boolean seSuperpone(Intervalo a, Intervalo b) {
        return a.dia == b.dia && a.inicio.isBefore(b.fin) && b.inicio.isBefore(a.fin);
    }

    private DiaSemana parsearDia(String valor) {
        String normalizado = normalizar(valor).toUpperCase();
        return switch (normalizado) {
            case "LUNES" -> DiaSemana.LUNES;
            case "MARTES" -> DiaSemana.MARTES;
            case "MIERCOLES" -> DiaSemana.MIERCOLES;
            case "JUEVES" -> DiaSemana.JUEVES;
            case "VIERNES" -> DiaSemana.VIERNES;
            default -> throw new ErrorFilaException("Día inválido: '" + valor + "'.");
        };
    }

    private LocalTime parsearHora(String valor, String etiqueta) {
        try {
            return LocalTime.parse(valor, FORMATO_HORA);
        } catch (DateTimeParseException e) {
            throw new ErrorFilaException(
                    "La " + etiqueta + " debe tener formato HH:mm (valor recibido: '" + valor + "').");
        }
    }

    private String normalizar(String valor) {
        String sinAcentos = Normalizer.normalize(valor == null ? "" : valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase();
    }

    // ------------------------------------------------------------------
    // Persistencia
    // ------------------------------------------------------------------

    private int persistir(List<FilaPlanificada> filas) {
        Map<String, AsignacionCurso> cache = new HashMap<>();
        int creados = 0;

        for (FilaPlanificada fila : filas) {
            String clave = fila.curso.getId() + ":" + fila.seccion.getId();
            AsignacionCurso asignacion = cache.get(clave);
            if (asignacion == null) {
                asignacion = asignacionRepo
                        .findByCursoIdAndSeccionId(fila.curso.getId(), fila.seccion.getId())
                        .orElseGet(() -> {
                            AsignacionCurso nueva = new AsignacionCurso();
                            nueva.setCurso(fila.curso);
                            nueva.setDocente(fila.docente);
                            nueva.setSeccion(fila.seccion);
                            return asignacionRepo.save(nueva);
                        });
                cache.put(clave, asignacion);
            }

            BloqueHorario bloque = new BloqueHorario();
            bloque.setDiaSemana(fila.dia);
            bloque.setHoraInicio(fila.inicio);
            bloque.setHoraFin(fila.fin);
            bloque.setAula(fila.aula);
            bloque.setAsignacionCurso(asignacion);
            bloqueRepo.save(bloque);
            creados++;
        }

        return creados;
    }

    // ------------------------------------------------------------------
    // Tipos internos
    // ------------------------------------------------------------------

    private static class ErrorFilaException extends RuntimeException {
        ErrorFilaException(String mensaje) {
            super(mensaje);
        }
    }

    private static class Intervalo {
        final DiaSemana dia;
        final LocalTime inicio;
        final LocalTime fin;

        Intervalo(DiaSemana dia, LocalTime inicio, LocalTime fin) {
            this.dia = dia;
            this.inicio = inicio;
            this.fin = fin;
        }
    }

    private static class FilaPlanificada {
        final Curso curso;
        final Seccion seccion;
        final Docente docente;
        final Aula aula;
        final DiaSemana dia;
        final LocalTime inicio;
        final LocalTime fin;

        FilaPlanificada(Curso curso, Seccion seccion, Docente docente, Aula aula,
                        DiaSemana dia, LocalTime inicio, LocalTime fin) {
            this.curso = curso;
            this.seccion = seccion;
            this.docente = docente;
            this.aula = aula;
            this.dia = dia;
            this.inicio = inicio;
            this.fin = fin;
        }
    }
}