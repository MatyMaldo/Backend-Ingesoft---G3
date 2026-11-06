package com.educoders.Gestion_Matriz_Horarios.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.SeccionRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/**
 * @deprecated Usar {@code AnioAcademicoController#obtenerPeriodoMatricula(Long)}
 * para obtener el estado del periodo de matrícula y las ventanas habilitadas.
 * Se mantiene temporalmente por compatibilidad con el frontend.
 */
@Deprecated(since = "2026-11", forRemoval = false)
@RestController
@RequestMapping("/api/v1/visualizadores-horario")
public class EstadoProgramacionController {

    private final SeccionRepository seccionRepo;
    private final PeriodoMatriculaRepository periodoRepo;

    public EstadoProgramacionController(
            SeccionRepository seccionRepo,
            PeriodoMatriculaRepository periodoRepo) {
        this.seccionRepo = seccionRepo;
        this.periodoRepo = periodoRepo;
    }

    public record EstadoProgramacionResponse(
            Long seccionId,
            Integer anioAcademico,
            boolean programacionAbierta
    ) {}

    @Transactional(readOnly = true)
    @GetMapping("/estado-programacion/{seccionId}")
    public EstadoProgramacionResponse consultar(
            @PathVariable Long seccionId) {

        var autenticacion = SecurityContextHolder
                .getContext().getAuthentication();

        if (autenticacion == null
                || !autenticacion.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        if (!(autenticacion.getPrincipal() instanceof Administrador)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo administradores");
        }

        var seccion = seccionRepo.findById(seccionId)
                .orElseThrow(() ->
                        RecursoNoEncontradoException.de(
                                "seccion", seccionId));

        var anio = seccion.getAnioAcademico();

        boolean abierta = periodoRepo
                .findByAnioAcademicoId(anio.getId())
                .map(periodo ->
                        !LocalDate.now().isAfter(periodo.getFechaFin()))
                .orElse(false);

        return new EstadoProgramacionResponse(
                seccionId,
                anio.getAnio(),
                abierta
        );
    }
}
