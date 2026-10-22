package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.AnioAcademico;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.AnioAcademicoRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.AnioAcademicoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnioAcademicoServiceImpl implements AnioAcademicoService {

    private final AnioAcademicoRepository anioAcademicoRepository;

    public AnioAcademicoServiceImpl(AnioAcademicoRepository anioAcademicoRepository) {
        this.anioAcademicoRepository = anioAcademicoRepository;
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
}
