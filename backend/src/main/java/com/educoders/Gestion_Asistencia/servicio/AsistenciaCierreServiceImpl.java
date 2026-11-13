package com.educoders.Gestion_Asistencia.servicio;

import com.educoders.Gestion_Asistencia.dominio.AsistenciaDocente;
import com.educoders.Gestion_Asistencia.dominio.AsistenciaEstudiante;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaDocenteRepository;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaEstudianteRepository;
import com.educoders.Gestion_Matricula_Pagos.dominio.Matricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Gestion_Matriz_Horarios.persistencia.SesionClaseRepository;
import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoSesion;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoError;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import com.educoders.Seguridad_Auditoria.servicio.AuditoriaService;
import com.educoders.Seguridad_Auditoria.servicio.LogErrorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AsistenciaCierreServiceImpl implements AsistenciaCierreService {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaCierreServiceImpl.class);

    private final SesionClaseRepository sesionRepository;
    private final AsistenciaEstudianteRepository asistenciaEstudianteRepository;
    private final AsistenciaDocenteRepository asistenciaDocenteRepository;
    private final MatriculaRepository matriculaRepository;
    private final AuditoriaService auditoriaService;
    private final LogErrorService logErrorService;
    private final TransactionTemplate transacciones;
    private final Clock clock;

    @Value("${app.asistencia.cierre.lote:200}")
    private int tamanoLote;

    public AsistenciaCierreServiceImpl(
            SesionClaseRepository sesionRepository,
            AsistenciaEstudianteRepository asistenciaEstudianteRepository,
            AsistenciaDocenteRepository asistenciaDocenteRepository,
            MatriculaRepository matriculaRepository,
            AuditoriaService auditoriaService,
            LogErrorService logErrorService,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.sesionRepository = sesionRepository;
        this.asistenciaEstudianteRepository = asistenciaEstudianteRepository;
        this.asistenciaDocenteRepository = asistenciaDocenteRepository;
        this.matriculaRepository = matriculaRepository;
        this.auditoriaService = auditoriaService;
        this.logErrorService = logErrorService;
        this.transacciones = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Override
    public int procesarSesiones() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<SesionClase> sesiones = sesionRepository.buscarParaCierre(
                EstadoSesion.ACTIVA,
                EstadoSesion.PROGRAMADA,
                ahora,
                PageRequest.of(0, tamanoLote));

        int procesadas = 0;
        for (SesionClase sesion : sesiones) {
            try {
                transacciones.executeWithoutResult(estado ->
                        procesarSesion(sesion.getId()));
                procesadas++;
            } catch (Exception ex) {
                log.error("No se pudo procesar la sesión {}", sesion.getId(), ex);
                registrarError(sesion.getId(), ex);
            }
        }
        return procesadas;
    }

    @Override
    @Transactional
    public void procesarSesion(Long sesionId) {
        SesionClase sesion = sesionRepository.findById(sesionId).orElse(null);
        if (sesion == null
                || sesion.getEstado() == EstadoSesion.OMITIDA
                || sesion.getEstado() == EstadoSesion.FINALIZADA) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!sesion.getHoraFin().isAfter(ahora)) {
            finalizarSesion(sesion);
        } else if (sesion.getEstado() == EstadoSesion.PROGRAMADA
                && !sesion.getHoraInicio().isAfter(ahora)) {
            sesion.setEstado(EstadoSesion.ACTIVA);
            sesionRepository.save(sesion);
        }
    }

    private void finalizarSesion(SesionClase sesion) {
        sesion.setEstado(EstadoSesion.FINALIZADA);
        sesionRepository.save(sesion);

        Long seccionId = sesion.getBloqueHorario()
                .getAsignacionCurso().getSeccion().getId();
        List<Matricula> roster = matriculaRepository
                .buscarPorSeccionYEstadoConEstudiante(
                        seccionId, EstadoMatricula.CONFIRMADA);

        Map<Long, AsistenciaEstudiante> existentes = new HashMap<>();
        for (AsistenciaEstudiante asistencia :
                asistenciaEstudianteRepository.findBySesionClaseId(sesion.getId())) {
            existentes.put(asistencia.getEstudiante().getId(), asistencia);
        }

        List<AsistenciaEstudiante> faltantes = new ArrayList<>();
        for (Matricula matricula : roster) {
            Long estudianteId = matricula.getEstudiante().getId();
            if (!existentes.containsKey(estudianteId)) {
                AsistenciaEstudiante falta = new AsistenciaEstudiante();
                falta.setSesionClase(sesion);
                falta.setEstudiante(matricula.getEstudiante());
                falta.setEstado(EstadoAsistencia.FALTA);
                falta.setFechaHoraRegistro(sesion.getHoraFin());
                faltantes.add(falta);
            }
        }
        if (!faltantes.isEmpty()) {
            asistenciaEstudianteRepository.saveAll(faltantes);
        }

        if (asistenciaDocenteRepository.findBySesionClaseId(sesion.getId()).isEmpty()) {
            AsistenciaDocente faltaDocente = new AsistenciaDocente();
            faltaDocente.setSesionClase(sesion);
            faltaDocente.setDocente(sesion.getBloqueHorario()
                    .getAsignacionCurso().getDocente());
            faltaDocente.setEstado(EstadoAsistencia.FALTA);
            faltaDocente.setFechaHoraRegistro(sesion.getHoraFin());
            asistenciaDocenteRepository.save(faltaDocente);
        }

        auditarFinalizacion(sesion, faltantes.size());
    }

    private void auditarFinalizacion(SesionClase sesion, int faltasCreadas) {
        try {
            auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                    null,
                    CategoriaAuditoria.ASISTENCIA,
                    TipoAccionAuditoria.ACTUALIZAR,
                    "SesionClase",
                    sesion.getId(),
                    "Cierre automático de sesión. Faltas creadas: " + faltasCreadas,
                    List.of()
            ));
        } catch (RuntimeException ex) {
            log.warn("No se pudo auditar el cierre de la sesión {}: {}",
                    sesion.getId(), ex.getMessage());
        }
    }

    private void registrarError(Long sesionId, Exception ex) {
        try {
            logErrorService.registrarError(
                    AsistenciaCierreServiceImpl.class.getName(),
                    TipoError.PROCESO_AUTOMATICO,
                    "Error al cerrar la sesión " + sesionId,
                    ex.getMessage(),
                    "/api/v1/asistencias/cierre",
                    "JOB",
                    (short) 500,
                    null);
        } catch (RuntimeException logEx) {
            log.warn("No se pudo registrar el error del cierre: {}", logEx.getMessage());
        }
    }
}
