package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoPagoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoPensionRequest;

public interface CompromisoPagoService {

    /**
     * HU-GEMAP-04: Emisión de compromiso de pago de matrícula para una matrícula en estado Pendiente.
     */
    CompromisoPagoResponse emitirCompromisoMatricula(EmitirCompromisoMatriculaRequest request);

    /**
     * HU-GEMAP-05: Emisión de compromiso de pago de pensión mensual.
     */
    CompromisoPagoResponse emitirCompromisoPension(EmitirCompromisoPensionRequest request);
}