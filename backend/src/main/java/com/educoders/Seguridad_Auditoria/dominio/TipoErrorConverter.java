package com.educoders.Seguridad_Auditoria.dominio;

import com.educoders.Infraestructura_Servicios.enums.TipoError;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el {@link TipoError} en {@code logs_error.tipo_error} con su etiqueta legible. */
@Converter
public class TipoErrorConverter implements AttributeConverter<TipoError, String> {

    @Override
    public String convertToDatabaseColumn(TipoError tipo) {
        return tipo == null ? null : tipo.getEtiqueta();
    }

    @Override
    public TipoError convertToEntityAttribute(String etiqueta) {
        return etiqueta == null ? null : TipoError.desdeEtiqueta(etiqueta);
    }
}
