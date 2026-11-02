
package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.LibretaNotasRepository;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.LibretaNotasResumenResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.LibretaNotasDetalleResponse;
import com.educoders.Gestion_Matricula_Pagos.persistencia.MatriculaRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoLibreta;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.educoders.Gestion_Academica_Motor_Evaluacion.persistencia.NotaCompetenciaRepository;
import java.io.IOException;
import java.util.List;

@Service
public class LibretaNotasServiceImpl implements LibretaNotasService {

    private final LibretaNotasRepository libretaNotasRepository;
    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final NotaCompetenciaRepository notaCompetenciaRepository;
    private final LibretaNotasPdfGenerator libretaNotasPdfGenerator;


    public LibretaNotasServiceImpl(
            LibretaNotasRepository libretaNotasRepository,
            EstudianteRepository estudianteRepository,
            MatriculaRepository matriculaRepository,
            NotaCompetenciaRepository notaCompetenciaRepository,
            LibretaNotasPdfGenerator libretaNotasPdfGenerator) {

        this.libretaNotasRepository = libretaNotasRepository;
        this.estudianteRepository = estudianteRepository;
        this.matriculaRepository = matriculaRepository;
        this.notaCompetenciaRepository = notaCompetenciaRepository;
        this.libretaNotasPdfGenerator = libretaNotasPdfGenerator;
    }


    // Validar acceso del estudiante o apoderado.
    private void validarAcceso(Long idEstudiante, Usuario usuarioAutenticado) {

        if (usuarioAutenticado == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        if (usuarioAutenticado instanceof Estudiante) {
            if (!usuarioAutenticado.getId().equals(idEstudiante)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No puedes consultar libretas de otro estudiante");
            }

        } else if (usuarioAutenticado instanceof Apoderado) {
            boolean esResponsable = estudianteRepository
                    .existsByIdAndApoderadoId(
                            idEstudiante, usuarioAutenticado.getId());

            if (!esResponsable) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No eres responsable de este estudiante");
            }

        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo estudiantes y apoderados pueden consultar libretas");
        }
    }

    // HU-EVAL-04 y HU-EVAL-05: listar libretas generadas.
    @Override
    @Transactional(readOnly = true)
    public List<LibretaNotasResumenResponse> listarLibretas(
            Long idEstudiante,
            Long idAnioAcademico,
            Usuario usuarioAutenticado) {

        if (idEstudiante == null || idAnioAcademico == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe especificar estudiante y año académico");
        }

        validarAcceso(idEstudiante, usuarioAutenticado);

        boolean tieneMatricula = matriculaRepository
                .existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoIn(
                        idEstudiante,
                        idAnioAcademico,
                        List.of(EstadoMatricula.CONFIRMADA));

        if (!tieneMatricula) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El estudiante no tiene matrícula confirmada en este año");
        }

        return libretaNotasRepository
                .findByEstudiante_IdAndBimestre_AnioAcademico_IdAndEstadoOrderByBimestre_FechaFinDesc(
                        idEstudiante,
                        idAnioAcademico,
                        EstadoLibreta.GENERADA)
                .stream()
                .map(libreta -> new LibretaNotasResumenResponse(
                        libreta.getId(),
                        libreta.getEstudiante().getId(),
                        libreta.getBimestre().getAnioAcademico().getAnio(),
                        libreta.getBimestre().getNombre().name(),
                        libreta.getFechaGeneracion(),
                        libreta.getComentariosFinalesTutor()
                ))
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public LibretaNotasDetalleResponse obtenerDetalleLibreta(
            Long idLibreta,
            Usuario usuarioAutenticado) {

        // 1. Validar el identificador.
        if (idLibreta == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe especificar la libreta");
        }

        if (usuarioAutenticado == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado");
        }

        // 2. Buscar la libreta.
        var libreta = libretaNotasRepository.findById(idLibreta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la libreta"));

        Long idEstudiante = libreta.getEstudiante().getId();

        // 3. Validar que el usuario pueda consultarla.
        validarAcceso(idEstudiante, usuarioAutenticado);

        // 4. Verificar que la libreta esté generada.
        if (libreta.getEstado() != EstadoLibreta.GENERADA) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "La libreta todavía no está disponible");
        }

        // 5. Verificar la matrícula del año académico.
        Long idAnioAcademico = libreta.getBimestre()
                .getAnioAcademico().getId();

        boolean tieneMatricula = matriculaRepository
                .existsByEstudianteIdAndSeccionAnioAcademicoIdAndEstadoIn(
                        idEstudiante,
                        idAnioAcademico,
                        List.of(EstadoMatricula.CONFIRMADA));

        if (!tieneMatricula) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe matrícula confirmada para este año");
        }

        // 6. Obtener las notas del estudiante y bimestre.
        var notas = notaCompetenciaRepository
                .buscarNotasParaLibreta(
                        idEstudiante,
                        libreta.getBimestre().getId());

        // 7. Convertir las notas al DTO.
        var notasResponse = notas.stream()
                .map(nota -> new LibretaNotasDetalleResponse.NotaPorCompetencia(
                        nota.getCompetencia().getCurso().getNombre(),
                        nota.getCompetencia().getNombre(),
                        nota.getValor(),
                        nota.getDescripcion(),
                        nota.getConclusionDescriptiva()
                ))
                .toList();

        // 8. Devolver el detalle completo.
        return new LibretaNotasDetalleResponse(
                libreta.getId(),
                idEstudiante,
                libreta.getBimestre().getAnioAcademico().getAnio(),
                libreta.getBimestre().getNombre().name(),
                libreta.getFechaGeneracion(),
                libreta.getComentariosFinalesTutor(),
                notasResponse
        );
    }


    @Override
    @Transactional(readOnly = true)
    public byte[] descargarLibretaPdf(
            Long idLibreta,
            Usuario usuarioAutenticado) {

        // 1. Obtener el detalle de la libreta.
        // Incluye las validaciones de acceso,
        // matrícula confirmada y estado GENERADA.
        LibretaNotasDetalleResponse detalle =
                obtenerDetalleLibreta(
                        idLibreta,
                        usuarioAutenticado
                );

        // 2. Generar el documento PDF.
        try {
            return libretaNotasPdfGenerator.generarPdf(detalle);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo generar el PDF de la libreta",
                    e
            );
        }
    }

}
