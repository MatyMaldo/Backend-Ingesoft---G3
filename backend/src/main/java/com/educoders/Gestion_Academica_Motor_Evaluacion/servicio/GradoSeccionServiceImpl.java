package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Grado;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Seccion;

import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.GradoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ActualizarSeccionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearGradoRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.CrearSeccionRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.GradoResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.SeccionResponse;

import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.VacantesAnioResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.GradoConSeccionesResponse;
import java.util.Comparator;
@Service
/**
 * Implementación de los servicios de gestión de grados y secciones académicas.
 *
 * Gestiona el registro, consulta y actualización de información académica,
 * aplicando las validaciones y reglas de negocio correspondientes.
 */
public class GradoSeccionServiceImpl implements GradoSeccionService {

    private final GradoRepository gradoRepository;
    private final SeccionRepository seccionRepository;
    private final AnioAcademicoRepository anioAcademicoRepository;
    private final DocenteRepository docenteRepository;
    private final PeriodoMatriculaRepository periodoMatriculaRepository;
    private final MatriculaRepository matriculaRepository;
    private final ValidadorVentanaAcademica validadorVentana;

    public GradoSeccionServiceImpl(
            GradoRepository gradoRepository,
            SeccionRepository seccionRepository,
            AnioAcademicoRepository anioAcademicoRepository,
            DocenteRepository docenteRepository,
            PeriodoMatriculaRepository periodoMatriculaRepository,
            MatriculaRepository matriculaRepository,
            ValidadorVentanaAcademica validadorVentana) {

        this.gradoRepository = gradoRepository;
        this.seccionRepository = seccionRepository;
        this.anioAcademicoRepository = anioAcademicoRepository;
        this.docenteRepository = docenteRepository;
        this.periodoMatriculaRepository = periodoMatriculaRepository;
        this.matriculaRepository = matriculaRepository;
        this.validadorVentana = validadorVentana;
    }


    @Override
    @Transactional
    public GradoResponse crearGrado(CrearGradoRequest request) {
        if (gradoRepository.existsByNombre(request.getNombre())) {
            throw new ConflictoException("Ya existe un grado con el nombre: " + request.getNombre());
        }

        Grado grado = Grado.builder()
                .nombre(request.getNombre())
                .build();

        Grado guardado = gradoRepository.save(grado);

        return GradoResponse.builder()
                .id(guardado.getId())
                .nombre(guardado.getNombre())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradoResponse> listarGrados() {
        return gradoRepository.findAll().stream()
                .map(g -> GradoResponse.builder().id(g.getId()).nombre(g.getNombre()).build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GradoResponse obtenerGradoPorId(Long id) {
        Grado grado = gradoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grado no encontrado con ID: " + id));
        return GradoResponse.builder().id(grado.getId()).nombre(grado.getNombre()).build();
    }

    @Override
    @Transactional
    public SeccionResponse crearSeccion(CrearSeccionRequest request) {
        Grado grado = gradoRepository.findById(request.getGradoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Grado no encontrado con ID: " + request.getGradoId()));

        AnioAcademico anio = anioAcademicoRepository.findById(request.getAnioAcademicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Año académico no encontrado con ID: " + request.getAnioAcademicoId()));


        // Validar que el año académico esté en preparación o el periodo de matrícula esté vigente
        LocalDate hoy = LocalDate.now();

        var periodoMatricula = periodoMatriculaRepository
                .findByAnioAcademicoId(anio.getId())
                .orElse(null);

        boolean enPreparacion = false;

        if (periodoMatricula != null) {
            // Permitir si el periodo de matrícula está vigente
            if (periodoMatricula.estaVigente(hoy)) {
                enPreparacion = true;
            }
            // O si estamos antes del inicio de matrícula (fase de preparación)
            else if (hoy.isBefore(periodoMatricula.getFechaInicio())) {
                enPreparacion = anioAcademicoRepository
                        .findFirstByAnioLessThanOrderByAnioDesc(anio.getAnio())
                        .map(anioAnterior ->
                                anioAnterior.getFechaFin() != null
                                        && hoy.isAfter(anioAnterior.getFechaFin()))
                        .orElse(true);
            }
        }

        if (!enPreparacion) {
            throw new ReglaNegocioException(
                    "El registro de secciones solo está habilitado mientras el año académico está en preparación o el periodo de matrícula está vigente");
        }


        // Validar duplicado ignorando mayúsculas
        if (seccionRepository.existsByGradoIdAndAnioAcademicoIdAndNombreIgnoreCase(request.getGradoId(), request.getAnioAcademicoId(), request.getNombre())) {
            throw new ConflictoException("La sección '" + request.getNombre() + "' ya existe para este grado y año académico.");
        }



        Seccion seccion = Seccion.builder()
                .nombre(request.getNombre().toUpperCase(java.util.Locale.ROOT))
                .capacidadMaxima(30)
                .grado(grado)
                .anioAcademico(anio)
                .build();

        Seccion guardada = seccionRepository.save(seccion);
        return mapToSeccionResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeccionResponse> listarSecciones() {
        return seccionRepository.findAll().stream()
                .map(this::mapToSeccionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeccionResponse> listarSeccionesPorGradoYAnio(Long gradoId, Long anioId) {
        return seccionRepository.findByGradoIdAndAnioAcademicoId(gradoId, anioId).stream()
                .map(this::mapToSeccionResponse)
                .collect(Collectors.toList());
    }


    private SeccionResponse mapToSeccionResponse(Seccion s) {
        String tutorNombre = null;

        if (s.getTutor() != null && s.getTutor().getNombre() != null) {
            tutorNombre = s.getTutor().getNombre()
                    + " " + s.getTutor().getApellidoPaterno();
        }

        // Contar solo las matrículas confirmadas
        long matriculadosConfirmados = matriculaRepository
                .countBySeccionIdAndEstadoIn(
                        s.getId(),
                        List.of(EstadoMatricula.CONFIRMADA)
                );

        // Calcular las vacantes disponibles
        long vacantesDisponibles = Math.max(
                0L,
                (long) s.getCapacidadMaxima() - matriculadosConfirmados
        );

        SeccionResponse response = SeccionResponse.builder()
                .id(s.getId())
                .nombre(s.getNombre())
                .capacidadMaxima(s.getCapacidadMaxima())
                .gradoId(s.getGrado().getId())
                .gradoNombre(s.getGrado().getNombre())
                .anioAcademicoId(s.getAnioAcademico().getId())
                .anioAcademico(s.getAnioAcademico().getAnio())
                .tutorId(s.getTutor() != null ? s.getTutor().getId() : null)
                .tutorNombreCompleto(tutorNombre)
                .build();

        response.setMatriculadosConfirmados(matriculadosConfirmados);
        response.setVacantesDisponibles(vacantesDisponibles);

        return response;
    }


    @Override
    @Transactional
    public SeccionResponse actualizarSeccion(Long id, ActualizarSeccionRequest request) {
        Seccion seccion = seccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sección no encontrada con ID: " + id));

        // Validar que la ventana académica esté abierta para modificar tutor
        validadorVentana.verificarVentanaAbierta(
                "No se puede modificar el tutor. El año académico ya inició."
        );

        // Validar y actualizar nombre
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            if (!request.getNombre().equals(seccion.getNombre())) {
                if (seccionRepository.existsByGradoIdAndAnioAcademicoIdAndNombreIgnoreCaseAndIdNot(
                        seccion.getGrado().getId(),
                        seccion.getAnioAcademico().getId(),
                        request.getNombre(),
                        id)) {
                    throw new ConflictoException("La sección '" + request.getNombre() + "' ya existe para este grado y año académico.");
                }
                seccion.setNombre(request.getNombre());
            }
        }

        // Validar y actualizar capacidad máxima
        if (request.getCapacidadMaxima() != null) {

            seccion.setCapacidadMaxima(request.getCapacidadMaxima());
        }

        // Validar y actualizar tutor
        if (request.tieneTutorIdPresente()) {
            if (request.getTutorId() == null) {
                seccion.setTutor(null);
            } else {
                Docente tutor = docenteRepository.findById(request.getTutorId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Docente tutor no encontrado con ID: " + request.getTutorId()));

                if (!tutor.isActivo()) {
                    throw new ReglaNegocioException(
                            "No se puede asignar un docente inactivo como tutor.");
                }

                if (seccionRepository.existsByAnioAcademicoIdAndTutorIdAndIdNot(
                        seccion.getAnioAcademico().getId(), request.getTutorId(), id)) {
                    throw new ConflictoException(
                            "El docente ya es tutor de otra sección en el mismo año académico.");
                }

                seccion.setTutor(tutor);
            }
        }

        Seccion actualizada = seccionRepository.save(seccion);
        return mapToSeccionResponse(actualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public VacantesAnioResponse consultarVacantesAnio(Long anioId) {

        AnioAcademico anio = anioAcademicoRepository.findById(anioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Año académico no encontrado con ID: " + anioId));

        List<Seccion> secciones =
                seccionRepository.findByAnioAcademicoId(anioId);

        long capacidadTotal = 0;
        long matriculasOcupantes = 0;

        for (Seccion seccion : secciones) {
            capacidadTotal += seccion.getCapacidadMaxima();

            matriculasOcupantes += matriculaRepository
                    .countBySeccionIdAndEstadoIn(
                            seccion.getId(),
                            List.of(
                                    EstadoMatricula.PENDIENTE,
                                    EstadoMatricula.CONFIRMADA
                            )
                    );
        }

        long vacantesDisponibles =
                Math.max(0L, capacidadTotal - matriculasOcupantes);

        return new VacantesAnioResponse(
                anio.getId(),
                anio.getAnio(),
                capacidadTotal,
                matriculasOcupantes,
                vacantesDisponibles
        );
    }


    @Override
    @Transactional(readOnly = true)
    public List<GradoConSeccionesResponse> listarGradosConSecciones(Long anioId) {

        // Verificar que el año académico exista
        anioAcademicoRepository.findById(anioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Año académico no encontrado con ID: " + anioId));

        // Obtener todos los grados, incluidos los que no tienen secciones
        List<Grado> grados = gradoRepository.findAll();

        return grados.stream()
                .sorted(Comparator.comparing(
                        Grado::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(grado -> {

                    List<SeccionResponse> secciones =
                            seccionRepository
                                    .findByGradoIdAndAnioAcademicoId(
                                            grado.getId(), anioId)
                                    .stream()
                                    .sorted(Comparator.comparing(
                                            Seccion::getNombre,
                                            Comparator.nullsLast(
                                                    String.CASE_INSENSITIVE_ORDER)
                                    ))
                                    .map(this::mapToSeccionResponse)
                                    .collect(Collectors.toList());

                    String estado = secciones.isEmpty()
                            ? "Sin secciones"
                            : "Con secciones";

                    return new GradoConSeccionesResponse(
                            grado.getId(),
                            grado.getNombre(),
                            secciones,
                            estado
                    );
                })
                .collect(Collectors.toList());
    }


}