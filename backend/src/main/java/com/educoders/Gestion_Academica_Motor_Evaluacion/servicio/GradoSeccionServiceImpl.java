package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.*;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.*;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.*;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GradoSeccionServiceImpl implements GradoSeccionService {

    private final GradoRepository gradoRepository;
    private final SeccionRepository seccionRepository;
    private final AnioAcademicoRepository anioAcademicoRepository;
    private final DocenteRepository docenteRepository;

    public GradoSeccionServiceImpl(GradoRepository gradoRepository,
                                   SeccionRepository seccionRepository,
                                   AnioAcademicoRepository anioAcademicoRepository,
                                   DocenteRepository docenteRepository) {
        this.gradoRepository = gradoRepository;
        this.seccionRepository = seccionRepository;
        this.anioAcademicoRepository = anioAcademicoRepository;
        this.docenteRepository = docenteRepository;
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

        // Validar que el año académico no esté finalizado
        if (anio.getFechaFin() != null && anio.getFechaFin().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("No se pueden crear secciones en un año académico finalizado.");
        }

        // Validar capacidad máxima (1-65535)
        if (request.getCapacidadMaxima() < 1 || request.getCapacidadMaxima() > 65535) {
            throw new ReglaNegocioException("La capacidad máxima debe estar entre 1 y 65535.");
        }

        // Validar duplicado ignorando mayúsculas
        if (seccionRepository.existsByGradoIdAndAnioAcademicoIdAndNombreIgnoreCase(request.getGradoId(), request.getAnioAcademicoId(), request.getNombre())) {
            throw new ConflictoException("La sección '" + request.getNombre() + "' ya existe para este grado y año académico.");
        }

        Docente tutor = null;
        if (request.getTutorId() != null) {
            tutor = docenteRepository.findById(request.getTutorId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Docente tutor no encontrado con ID: " + request.getTutorId()));

            if (seccionRepository.existsByAnioAcademicoIdAndTutorId(request.getAnioAcademicoId(), request.getTutorId())) {
                throw new ConflictoException("El docente ya es tutor de otra sección en el mismo año académico.");
            }
        }

        Seccion seccion = Seccion.builder()
                .nombre(request.getNombre())
                .capacidadMaxima(request.getCapacidadMaxima())
                .grado(grado)
                .anioAcademico(anio)
                .tutor(tutor)
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
            tutorNombre = s.getTutor().getNombre() + " " + s.getTutor().getApellidoPaterno();
        }

        return SeccionResponse.builder()
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
    }
}