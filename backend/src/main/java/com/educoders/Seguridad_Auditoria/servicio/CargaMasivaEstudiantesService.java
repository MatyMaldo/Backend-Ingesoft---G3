
package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaEstudiantesResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarEstudianteRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gestiona la carga masiva de estudiantes desde archivos CSV.
 * HU-GEMAP-02: registro masivo transaccional.
 */
@Service
public class CargaMasivaEstudiantesService {

    private static final long TAMANO_MAXIMO_BYTES = 2 * 1024 * 1024;

    private final EstudianteCsvParser csvParser;
    private final EstudianteService estudianteService;
    private final Validator validator;

    public CargaMasivaEstudiantesService(
            EstudianteCsvParser csvParser,
            EstudianteService estudianteService,
            Validator validator) {

        this.csvParser = csvParser;
        this.estudianteService = estudianteService;
        this.validator = validator;
    }

    /**
     * Registra todos los estudiantes del CSV en una única transacción.
     * Si falla cualquier registro, se revierten todos los cambios.
     */
    @Transactional
    public CargaMasivaEstudiantesResponse cargarEstudiantes(
            MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException(
                    "Debe seleccionar un archivo CSV no vacío.");
        }

        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ReglaNegocioException(
                    "El archivo CSV no debe superar los 2 MB.");
        }

        List<EstudianteCsvParser.FilaEstudianteCsv> filas;

        try (InputStream entrada = archivo.getInputStream()) {
            filas = csvParser.leer(entrada);
        } catch (IOException ex) {
            throw new ReglaNegocioException(
                    "No se pudo abrir el archivo CSV.");
        }

        for (int i = 0; i < filas.size(); i++) {

            EstudianteCsvParser.FilaEstudianteCsv fila = filas.get(i);
            int numeroFila = i + 2;

            // Código autogenerado sin exponer el DNI del estudiante.
            String codigo = "EST-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 16)
                    .toUpperCase();

            RegistrarEstudianteRequest solicitud =
                    new RegistrarEstudianteRequest(
                            fila.nombre(),
                            fila.apellidoPaterno(),
                            normalizarOpcional(fila.apellidoMaterno()),
                            fila.dni(),
                            fila.email(),
                            normalizarOpcional(fila.telefono()),
                            codigo,
                            fila.fechaNacimiento(),
                            fila.apoderadoId()
                    );

            validarSolicitud(solicitud, numeroFila);

            try {
                // Reutiliza la lógica del registro individual.
                estudianteService.registrarEstudiante(solicitud);

            } catch (ConflictoException |
                     RecursoNoEncontradoException ex) {

                throw new ReglaNegocioException(
                        "Error en la fila " + numeroFila
                                + ": " + ex.getMessage());
            }
        }

        return new CargaMasivaEstudiantesResponse(
                filas.size(),
                "Carga masiva realizada correctamente."
        );
    }

    /**
     * Aplica las mismas validaciones del DTO de registro individual.
     */
    private void validarSolicitud(
            RegistrarEstudianteRequest solicitud,
            int numeroFila) {

        Set<ConstraintViolation<RegistrarEstudianteRequest>> errores =
                validator.validate(solicitud);

        if (!errores.isEmpty()) {
            String mensaje = errores.iterator()
                    .next()
                    .getMessage();

            throw new ReglaNegocioException(
                    "Error en la fila " + numeroFila + ": " + mensaje);
        }
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
