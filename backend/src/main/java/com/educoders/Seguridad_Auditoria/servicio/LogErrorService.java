package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.presentacion.dto.LogErrorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface LogErrorService {

    void registrarError(String origen, String tipoError, String mensaje, String detalle,
                        String endpoint, String metodoHttp,
                        Short codigoEstado, Long usuarioId);

    Page<LogErrorResponse> listarErrores(LocalDateTime fechaInicio,
                                         LocalDateTime fechaFin, Pageable pageable);
}