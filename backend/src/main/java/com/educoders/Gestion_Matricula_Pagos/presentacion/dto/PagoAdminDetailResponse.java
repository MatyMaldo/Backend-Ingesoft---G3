package com.educoders.Gestion_Matricula_Pagos.presentacion.dto;

import com.educoders.Gestion_Matricula_Pagos.dominio.Pago;
import com.educoders.Infraestructura_Servicios.enums.EstadoPago;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoAdminDetailResponse(
        Long id,
        LocalDate fechaRegistro,
        BigDecimal montoPagado,
        EstadoPago estado,
        String evidenciaUrl,
        String motivo,

        // Información del Compromiso
        Long compromisoId,
        TipoCompromiso tipoCompromiso,

        // Información del Estudiante
        Long estudianteId,
        String estudianteNombreCompleto,
        String gradoSeccion,

        // Información del Apoderado Pagador
        Long apoderadoId,
        String apoderadoNombreCompleto,

        // Información del Validador (si ya fue procesado)
        Long validadorId,
        String validadorNombreCompleto
) {
    public static PagoAdminDetailResponse fromEntity(Pago p) {
        var compromiso = p.getCompromisoPago();
        var matricula = compromiso.getMatricula();
        var estudiante = matricula.getEstudiante();
        var apoderado = p.getApoderadoPagador();
        var validador = p.getUsuarioValidador();

        String nombreEstudiante = estudiante.getNombre() + " " +
                estudiante.getApellidoPaterno() + " " + estudiante.getApellidoMaterno();

        String nombreApoderado = apoderado.getNombre() + " " +
                apoderado.getApellidoPaterno() + " " + apoderado.getApellidoMaterno();

        String gradoSeccionStr = matricula.getSeccion().getGrado().getNombre() + " - " +
                matricula.getSeccion().getNombre();

        String nombreValidador = validador != null
                ? validador.getNombre() + " " + validador.getApellidoPaterno()
                : null;

        return new PagoAdminDetailResponse(
                p.getId(),
                p.getFechaRegistro(),
                p.getMontoPagado(),
                p.getEstado(),
                p.getEvidenciaUrl(),
                p.getMotivo(),
                compromiso.getId(),
                compromiso.getTipoCompromiso(),
                estudiante.getId(),
                nombreEstudiante,
                gradoSeccionStr,
                apoderado.getId(),
                nombreApoderado,
                validador != null ? validador.getId() : null,
                nombreValidador
        );
    }
}