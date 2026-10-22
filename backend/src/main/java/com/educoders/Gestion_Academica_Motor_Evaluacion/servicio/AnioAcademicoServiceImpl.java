package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.AnioAcademicoResponse;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PeriodoMatriculaRepository;
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

    public AnioAcademicoServiceImpl(AnioAcademicoRepository anioAcademicoRepository,
                                     PeriodoMatriculaRepository periodoMatriculaRepository) {
        this.anioAcademicoRepository = anioAcademicoRepository;
        this.periodoMatriculaRepository = periodoMatriculaRepository;
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
}
