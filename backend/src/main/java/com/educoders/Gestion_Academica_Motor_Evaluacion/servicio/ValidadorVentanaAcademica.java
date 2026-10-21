package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.Bimestre;
import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.BimestreRepository;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ValidadorVentanaAcademica {

    private final BimestreRepository bimestreRepository;

    public ValidadorVentanaAcademica(BimestreRepository bimestreRepository) {
        this.bimestreRepository = bimestreRepository;
    }

    /**
     * Valida que la fecha actual sea anterior al inicio del BIM_1 del año más reciente.
     * Lanza excepción si la ventana está cerrada.
     * Con tal de respetar DRY (Don't Repeat Yourself)
     */
    public void verificarVentanaAbierta(String mensajeError) {
        Bimestre primerBimestre = bimestreRepository.findFirstByNombreOrderByAnioAcademicoAnioDesc("BIM_1")
                .orElseThrow(() -> new ReglaNegocioException("No hay un calendario académico configurado."));

        if (!LocalDate.now().isBefore(primerBimestre.getFechaInicio())) {
            throw new ReglaNegocioException(mensajeError);
        }
    }
}
