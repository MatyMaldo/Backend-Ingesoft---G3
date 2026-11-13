package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Bimestre;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.BimestreRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.AnioAcademicoResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.BimestreResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.PeriodoMatriculaEstadoResponse;
import com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula;
import com.educoders.Gestion_Matricula_Pagos.dominio.VentanaMatricula;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoPeriodoMatricula;
import com.educoders.Infraestructura_Servicios.enums.NombreBimestre;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AnioAcademicoServiceImpl implements AnioAcademicoService {

    private final AnioAcademicoRepository anioAcademicoRepository;
    private final PeriodoMatriculaRepository periodoMatriculaRepository;
    private final BimestreRepository bimestreRepository;

    public AnioAcademicoServiceImpl(AnioAcademicoRepository anioAcademicoRepository,
                                     PeriodoMatriculaRepository periodoMatriculaRepository,
                                     BimestreRepository bimestreRepository) {
        this.anioAcademicoRepository = anioAcademicoRepository;
        this.periodoMatriculaRepository = periodoMatriculaRepository;
        this.bimestreRepository = bimestreRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnioAcademicoResponse> listarAniosAcademicos() {
        return anioAcademicoRepository.findAll().stream()
                .map(a -> AnioAcademicoResponse.builder()
                        .id(a.getId())
                        .anio(a.getAnio())
                        .fechaInicio(a.getFechaInicio())
                        .fechaFin(a.getFechaFin())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnioAcademicoResponse> obtenerAnioAcademicoConPeriodoMatriculaVigente() {
        LocalDate hoy = LocalDate.now();

        // Buscar años académicos futuros ordenados por fecha de inicio
        List<AnioAcademico> aniosFuturos = anioAcademicoRepository.findByFechaInicioGreaterThanOrderByFechaInicioAsc(hoy);

        // Buscar el primer año futuro que tenga período de matrícula vigente
        for (AnioAcademico anio : aniosFuturos) {
            Optional<com.educoders.Gestion_Matricula_Pagos.dominio.PeriodoMatricula> periodo =
                    periodoMatriculaRepository.findByAnioAcademicoId(anio.getId());

            if (periodo.isPresent() && periodo.get().estaVigente(hoy)) {
                return Optional.of(AnioAcademicoResponse.builder()
                        .id(anio.getId())
                        .anio(anio.getAnio())
                        .fechaInicio(anio.getFechaInicio())
                        .fechaFin(anio.getFechaFin())
                        .build());
            }
        }

        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public PeriodoMatriculaEstadoResponse obtenerPeriodoMatricula(Long anioId) {
        AnioAcademico anio = anioAcademicoRepository.findById(anioId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Año académico", anioId));

        PeriodoMatricula periodo = periodoMatriculaRepository.findByAnioAcademicoId(anioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró un periodo de matrícula para el año académico "
                                + anio.getAnio() + "."));

        LocalDate hoy = LocalDate.now();
        EstadoPeriodoMatricula estado = VentanaMatricula.estado(periodo, hoy);

        Bimestre primerBimestre = bimestreRepository
                .findFirstByNombreAndAnioAcademicoIdOrderByAnioAcademicoAnioDesc(
                        NombreBimestre.BIM_1, anioId)
                .orElse(null);

        boolean generacionSesionesAbierta = primerBimestre != null
                && VentanaMatricula.generacionSesionesAbierta(
                        periodo.getFechaFin(), primerBimestre.getFechaInicio(), hoy);

        return new PeriodoMatriculaEstadoResponse(
                periodo.getId(),
                anio.getId(),
                anio.getAnio(),
                periodo.getFechaInicio(),
                periodo.getFechaFin(),
                estado,
                VentanaMatricula.asignacionCursoDocenteAbierta(periodo, hoy),
                VentanaMatricula.crudBloquesHorarioAbierta(periodo, hoy),
                generacionSesionesAbierta
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BimestreResponse> listarBimestres(Long anioId) {
        anioAcademicoRepository.findById(anioId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Año académico", anioId));

        return bimestreRepository.findByAnioAcademicoIdOrderByFechaInicioAsc(anioId)
                .stream()
                .map(bimestre -> new BimestreResponse(
                        bimestre.getId(),
                        bimestre.getNombre(),
                        bimestre.getFechaInicio(),
                        bimestre.getFechaFin()))
                .toList();
    }
}
