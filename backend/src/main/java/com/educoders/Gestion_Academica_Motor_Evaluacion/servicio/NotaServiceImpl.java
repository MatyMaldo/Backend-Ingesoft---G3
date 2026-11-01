package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Evaluacion;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Nota;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.EvaluacionRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.NotaRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.NotaResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.RegistrarNotaRequest;
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotaServiceImpl implements NotaService {

    private final NotaRepository notaRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final AuditoriaService auditoriaService;

    public NotaServiceImpl(NotaRepository notaRepository,
                           EvaluacionRepository evaluacionRepository,
                           EstudianteRepository estudianteRepository,
                           MatriculaRepository matriculaRepository,
                           AuditoriaService auditoriaService) {
        this.notaRepository = notaRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.estudianteRepository = estudianteRepository;
        this.matriculaRepository = matriculaRepository;
        this.auditoriaService = auditoriaService;
    }

    /**
     * Registra o modifica la nota de un estudiante en una evaluación.
     * Regla de negocio: HU-EVAL-02. Valida que el docente sea el dueño del curso,
     * que el año esté vigente y que el estudiante tenga matrícula CONFIRMADA.
     */
    @Override
    @Transactional
    public NotaResponse registrarOActualizarNota(Long idEvaluacion, RegistrarNotaRequest request, Docente docenteLogueado) {

        // 1. Validar que la evaluación existe y pertenece al docente
        Evaluacion evaluacion = evaluacionRepository.findById(idEvaluacion)
                .orElseThrow(() -> new ReglaNegocioException("La evaluación no existe."));

        if (!evaluacion.getAsignacionCurso().getDocente().getId().equals(docenteLogueado.getId())) {
            throw new ReglaNegocioException("No tiene permiso para registrar notas en esta evaluación.");
        }

        // 2. Validar año académico vigente
        if (!evaluacion.getAsignacionCurso().getSeccion().getAnioAcademico().estaVigente()) {
            throw new ReglaNegocioException("Las notas solo se registran con el año académico vigente.");
        }

        // 3. Validar que el estudiante existe y está activo
        Estudiante estudiante = estudianteRepository.findById(request.idEstudiante())
                .orElseThrow(() -> new ReglaNegocioException("El estudiante no existe."));

        if (!estudiante.isActivo()) {
            throw new ReglaNegocioException("No se puede registrar notas a un estudiante inactivo.");
        }

        // 4. Validar que el estudiante tiene matrícula CONFIRMADA en la sección de la evaluación
        Long idSeccion = evaluacion.getAsignacionCurso().getSeccion().getId();
        Matricula matricula = matriculaRepository.findByEstudianteIdAndSeccionId(estudiante.getId(), idSeccion)
                .orElseThrow(() -> new ReglaNegocioException("El estudiante no está matriculado en esta sección."));

        if (matricula.getEstado() != EstadoMatricula.CONFIRMADA) {
            throw new ReglaNegocioException("El estudiante no tiene una matrícula confirmada.");
        }

        // 5. Buscar si ya existe la nota (Modificación vs Registro Nuevo)
        Nota nota = notaRepository.findByEstudianteIdAndEvaluacionId(estudiante.getId(), idEvaluacion)
                .orElseGet(() -> {
                    Nota nuevaNota = new Nota();
                    nuevaNota.setEstudiante(estudiante);
                    nuevaNota.setEvaluacion(evaluacion);
                    return nuevaNota;
                });

        // 6. Validar que no sea la misma nota si es una modificación (para auditar)
        boolean esModificacion = nota.getId() != null;
        if (esModificacion && nota.getValor() == request.valorNota()) {
            throw new ReglaNegocioException("El estudiante ya tiene registrada esta nota en la evaluación.");
        }

        nota.setValor(request.valorNota());
        nota.setFechaRegistro(LocalDate.now());

        Nota notaGuardada = notaRepository.save(nota);

        // 7. Registrar en Auditoría (HU-SAT-04)
        TipoAccionAuditoria tipoAccion = esModificacion ? TipoAccionAuditoria.ACTUALIZAR : TipoAccionAuditoria.CREAR;
        String descripcion = esModificacion ? "Modificación de nota de evaluación" : "Registro de nota de evaluación";
        RegistrarAuditoriaRequestDTO auditoriaDTO = new RegistrarAuditoriaRequestDTO(
                docenteLogueado.getId(),
                CategoriaAuditoria.NOTAS,
                tipoAccion,
                "Nota",
                notaGuardada.getId(),
                descripcion,
                new ArrayList<>()
        );
        auditoriaService.registrarAuditoria(auditoriaDTO);

        // 8. Retornar DTO
        return new NotaResponse(
                notaGuardada.getId(),
                evaluacion.getId(),
                evaluacion.getNombre(),
                estudiante.getId(),
                estudiante.getNombre() + " " + estudiante.getApellidoPaterno(),
                notaGuardada.getValor(),
                notaGuardada.getFechaRegistro()
        );
    }
}