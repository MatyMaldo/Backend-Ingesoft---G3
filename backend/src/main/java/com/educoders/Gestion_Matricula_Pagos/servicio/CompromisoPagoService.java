package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoApoderadoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.CompromisoPagoResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoMatriculaRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisoPensionRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosMatriculaLoteRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosMatriculaLoteResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosPensionLoteRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.EmitirCompromisosPensionLoteResponse;
import com.educoders.Infraestructura_Servicios.operaciones.ProgresoLote;

import java.util.List;

public interface CompromisoPagoService {

    /**
     * HU-GEMAP-04: Emisión de compromiso de pago de matrícula para una matrícula en estado Pendiente.
     */
    CompromisoPagoResponse emitirCompromisoMatricula(EmitirCompromisoMatriculaRequest request);

    /**
     * Emisión masiva de compromisos de matrícula para las matrículas
     * pendientes de un año académico.
     */
    EmitirCompromisosMatriculaLoteResponse emitirCompromisosMatriculaLote(
            EmitirCompromisosMatriculaLoteRequest request);

    /**
     * Variante usada por operaciones asíncronas. Recibe el usuario de forma
     * explícita para auditar sin depender del {@code SecurityContext} del hilo
     * del worker y reporta el avance mediante {@code progreso}.
     * <p>
     * Si {@code usuarioId} es {@code null} se mantiene la validación de
     * administrador del contexto actual (compatibilidad con el flujo síncrono).
     */
    EmitirCompromisosMatriculaLoteResponse emitirCompromisosMatriculaLote(
            EmitirCompromisosMatriculaLoteRequest request,
            Long usuarioId,
            ProgresoLote progreso);

    /**
     * Emisión masiva de compromisos de pensión para un año y mes.
     */
    EmitirCompromisosPensionLoteResponse emitirCompromisosPensionLote(
            EmitirCompromisosPensionLoteRequest request);

    /**
     * Variante asíncrona de la emisión masiva de pensión.
     */
    EmitirCompromisosPensionLoteResponse emitirCompromisosPensionLote(
            EmitirCompromisosPensionLoteRequest request,
            Long usuarioId,
            ProgresoLote progreso);

    /**
     * HU-GEMAP-05: Emisión de compromiso de pago de pensión mensual.
     */
    CompromisoPagoResponse emitirCompromisoPension(EmitirCompromisoPensionRequest request);

    /**
     * HU-GEMAP-07: Listado de compromisos de pago
     */
    List<CompromisoApoderadoResponse> listarCompromisosPorEstudianteYAnio(
            Long estudianteId,
            Integer anio
    );
}