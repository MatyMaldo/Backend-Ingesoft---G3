package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.PagoApoderadoResponse;

import java.util.List;

public interface PagoService {
    List<PagoApoderadoResponse> listarPagosPorCompromisoYApoderado(Long apoderadoId, Long compromisoId);
}