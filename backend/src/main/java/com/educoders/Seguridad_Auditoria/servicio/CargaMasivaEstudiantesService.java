
package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.persistencia.ApoderadoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaEstudiantesResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarEstudianteRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestiona la carga masiva de estudiantes desde archivos CSV.
 * HU-GEMAP-02: registro masivo transaccional.
 */
@Service
public class CargaMasivaEstudiantesService {

    private static final long TAMANO_MAXIMO_BYTES = 2 * 1024 * 1024;

    private final EstudianteCsvParser csvParser;
    private final EstudianteService estudianteService;
    private final UsuarioRepository usuarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final Validator validator;

    public CargaMasivaEstudiantesService(
            EstudianteCsvParser csvParser,
            EstudianteService estudianteService,
            UsuarioRepository usuarioRepository,
            ApoderadoRepository apoderadoRepository,
            Validator validator) {

        this.csvParser = csvParser;
        this.estudianteService = estudianteService;
        this.usuarioRepository = usuarioRepository;
        this.apoderadoRepository = apoderadoRepository;
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

        // Verificaciones de unicidad y carga de apoderados en pocas consultas,
        // en lugar de 4-5 consultas por cada fila.
        Set<String> dnisUsados = new HashSet<>(
                usuarioRepository.findDnisExistentesPorTipo(
                        filas.stream().map(f -> f.dni()).collect(Collectors.toSet()),
                        Estudiante.class));

        Set<String> emailsUsados = enMinusculas(
                usuarioRepository.findEmailsExistentes(
                        filas.stream().map(f -> f.email()).collect(Collectors.toSet())));

        Set<String> usernamesUsados = enMinusculas(
                usuarioRepository.findUsernamesExistentes(
                        filas.stream()
                                .map(f -> usernameDe(f.email()))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())));

        Map<Long, Apoderado> apoderados = apoderadoRepository
                .findAllById(filas.stream()
                        .map(f -> f.apoderadoId())
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Apoderado::getId, Function.identity()));

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

            // Mismos controles y mensajes que el registro individual, resueltos en memoria.
            // Al ir marcando lo ya visto también se detectan duplicados dentro del propio CSV.
            if (!dnisUsados.add(solicitud.dni())) {
                throw errorFila(numeroFila,
                        "Ya existe un estudiante registrado con el DNI indicado.");
            }

            if (!emailsUsados.add(solicitud.email().toLowerCase(Locale.ROOT))) {
                throw errorFila(numeroFila,
                        "Ya existe un usuario registrado con el correo indicado.");
            }

            if (!usernamesUsados.add(
                    usernameDe(solicitud.email()).toLowerCase(Locale.ROOT))) {
                throw errorFila(numeroFila,
                        "Ya existe un usuario registrado con ese identificador.");
            }

            Apoderado apoderado = apoderados.get(solicitud.apoderadoId());
            if (apoderado == null) {
                throw errorFila(numeroFila,
                        RecursoNoEncontradoException
                                .de("el apoderado", solicitud.apoderadoId())
                                .getMessage());
            }

            estudianteService.registrarEstudianteEnLote(solicitud, apoderado);
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

    private ReglaNegocioException errorFila(int numeroFila, String mensaje) {
        return new ReglaNegocioException(
                "Error en la fila " + numeroFila + ": " + mensaje);
    }

    /** Username = parte local del correo (antes del @); nulo si el correo no tiene @. */
    private String usernameDe(String email) {
        int arroba = email.indexOf('@');
        return arroba < 0 ? null : email.substring(0, arroba);
    }

    private Set<String> enMinusculas(Collection<String> valores) {
        return valores.stream()
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
