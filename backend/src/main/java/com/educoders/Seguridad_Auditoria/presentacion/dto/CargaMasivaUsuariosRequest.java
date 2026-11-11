package com.educoders.Seguridad_Auditoria.presentacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Carga masiva de usuarios: las filas ya leídas del archivo, en una sola petición.
 * <p>
 * Aquí solo se valida la forma del envío (lista no vacía, tope de filas, tipo informado). Los datos de cada fila
 * (DNI, correo, teléfono...) se validan en el servicio con las mismas reglas del registro individual y una fila
 * inválida se rechaza sola, sin tumbar el resto del lote.
 */
public record CargaMasivaUsuariosRequest(

        @NotEmpty(message = "El archivo no tiene filas para importar.")
        @Size(max = CargaMasivaUsuariosRequest.MAX_FILAS,
                message = "Una carga masiva admite como máximo " + CargaMasivaUsuariosRequest.MAX_FILAS + " filas.")
        List<@NotNull(message = "Una de las filas está vacía.") @Valid Fila> filas
) {

    public static final int MAX_FILAS = 1000;

    /** Tipo de usuario de una fila. */
    public enum TipoUsuario {
        APODERADO, DOCENTE, ESTUDIANTE
    }

    /**
     * Una fila del archivo. {@code linea} es el número de línea del archivo (para devolver el resultado de cada fila).
     * {@code edad} solo aplica a apoderados; {@code fechaNacimiento}, {@code dniApoderado} y {@code codigo}, a
     * estudiantes. El teléfono va como 9 dígitos, sin prefijo.
     */
    public record Fila(
            @NotNull(message = "Cada fila debe indicar su número de línea.") Integer linea,
            @NotNull(message = "Cada fila debe indicar su tipo de usuario.") TipoUsuario tipoUsuario,
            String nombre,
            String apellidoPaterno,
            String apellidoMaterno,
            String dni,
            String email,
            String telefono,
            Integer edad,
            LocalDate fechaNacimiento,
            String dniApoderado,
            String codigo
    ) {
    }
}
