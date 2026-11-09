package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Pago;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.Mes;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Compromiso de pago con su historial de pagos, para el listado agrupado del admin.
 */
public record CompromisoPagoAdminResponse(
        Long compromisoId,
        TipoCompromiso tipoCompromiso,
        Mes mes,
        LocalDate fechaEmision,
        BigDecimal montoOriginal,
        BigDecimal montoMora,
        BigDecimal montoTotal,
        EstadoCompromisoPago estadoCompromiso,
        Long matriculaId,
        Long estudianteId,
        String estudianteNombreCompleto,
        String codigoEstudiante,
        Long gradoId,
        String gradoNombre,
        Long seccionId,
        String seccionNombre,
        Long anioAcademicoId,
        Integer anioAcademico,
        List<PagoAdminListResponse> pagos
) {

    public static CompromisoPagoAdminResponse fromEntity(CompromisoPago compromiso,
                                                         List<Pago> pagos) {
        var matricula = compromiso.getMatricula();
        var estudiante = matricula.getEstudiante();
        var seccion = matricula.getSeccion();
        var grado = seccion.getGrado();
        var anio = seccion.getAnioAcademico();

        String nombreCompleto = estudiante.getNombre() + " " + estudiante.getApellidoPaterno();
        if (estudiante.getApellidoMaterno() != null && !estudiante.getApellidoMaterno().isBlank()) {
            nombreCompleto += " " + estudiante.getApellidoMaterno();
        }

        return new CompromisoPagoAdminResponse(
                compromiso.getId(),
                compromiso.getTipoCompromiso(),
                compromiso.getMes(),
                compromiso.getFechaEmision(),
                compromiso.getMontoOriginal(),
                compromiso.getMontoMora(),
                compromiso.calcularMontoTotal(),
                compromiso.getEstado(),
                matricula.getId(),
                estudiante.getId(),
                nombreCompleto,
                estudiante.getCodigo(),
                grado.getId(),
                grado.getNombre(),
                seccion.getId(),
                seccion.getNombre(),
                anio.getId(),
                anio.getAnio(),
                pagos.stream().map(PagoAdminListResponse::fromEntity).toList()
        );
    }
}
