
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

import com.educoders.Gestion_Academica_Motor_Evaluacion.dominio.LibretaNotas;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.GenerarLibretaNotasRequest;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.VistaPreviaLibretaNotasResponse;
import com.educoders.Gestion_Asistencia.persistencia.AsistenciaEstudianteRepository;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import org.springframework.beans.factory.annotation.Value;
import com.educoders.Infraestructura_Servicios.enums.EstadoAsistencia;
import com.educoders.Seguridad_Auditoria.dominio.Docente;

import java.time.LocalDate;


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


    private final ValidadorGeneracionLibreta validadorGeneracionLibreta;
    private final AsistenciaEstudianteRepository asistenciaEstudianteRepository;

    @Value("${app.institucion.nombre:Institución educativa}")
    private String nombreInstitucion;


    public LibretaNotasServiceImpl(
            LibretaNotasRepository libretaNotasRepository,
            EstudianteRepository estudianteRepository,
            MatriculaRepository matriculaRepository,
            NotaCompetenciaRepository notaCompetenciaRepository,
            LibretaNotasPdfGenerator libretaNotasPdfGenerator,
            ValidadorGeneracionLibreta validadorGeneracionLibreta,
            AsistenciaEstudianteRepository asistenciaEstudianteRepository) {

        this.libretaNotasRepository = libretaNotasRepository;
        this.estudianteRepository = estudianteRepository;
        this.matriculaRepository = matriculaRepository;
        this.notaCompetenciaRepository = notaCompetenciaRepository;
        this.libretaNotasPdfGenerator = libretaNotasPdfGenerator;
        this.validadorGeneracionLibreta = validadorGeneracionLibreta;
        this.asistenciaEstudianteRepository = asistenciaEstudianteRepository;
    }



    /**
     * HU-EVAL-04/05/06: Verifica que la libreta pueda
     * ser consultada por el estudiante, su apoderado
     * o el tutor de su sección en el año indicado.
     */
    private void validarAcceso(
            Long idEstudiante,
            Long idAnioAcademico,
            Usuario usuarioAutenticado) {

        if (usuarioAutenticado == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado");
        }

        if (usuarioAutenticado instanceof Estudiante) {

            if (!usuarioAutenticado.getId().equals(idEstudiante)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No puedes consultar libretas de otro estudiante");
            }

            return;
        }

        if (usuarioAutenticado instanceof Apoderado) {

            boolean esResponsable = estudianteRepository
                    .existsByIdAndApoderadoId(
                            idEstudiante,
                            usuarioAutenticado.getId()
                    );

            if (!esResponsable) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No eres responsable de este estudiante");
            }

            return;
        }

        if (usuarioAutenticado instanceof Docente tutor) {

            boolean esTutor = matriculaRepository
                    .buscarPorEstudianteYAnioId(
                            idEstudiante,
                            idAnioAcademico,
                            EstadoMatricula.CONFIRMADA
                    )
                    .stream()
                    .anyMatch(matricula ->
                            matricula.getSeccion().getTutor() != null
                                    && tutor.getId().equals(
                                    matricula.getSeccion()
                                            .getTutor().getId())
                    );

            if (!esTutor) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No eres tutor de la sección del estudiante");
            }

            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Usuario sin permisos para consultar libretas");
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

        validarAcceso(idEstudiante, idAnioAcademico, usuarioAutenticado);

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



    /**
     * HU-EVAL-04/05/06: Consulta la libreta generada,
     * incorporando notas, información académica y asistencia.
     */
    @Override
    @Transactional(readOnly = true)
    public LibretaNotasDetalleResponse obtenerDetalleLibreta(
            Long idLibreta,
            Usuario usuarioAutenticado) {

        if (idLibreta == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe especificar la libreta");
        }

        var libreta = libretaNotasRepository.findById(idLibreta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la libreta"));

        Long idEstudiante = libreta.getEstudiante().getId();
        var bimestre = libreta.getBimestre();
        Long idAnioAcademico = bimestre.getAnioAcademico().getId();

        validarAcceso(
                idEstudiante,
                idAnioAcademico,
                usuarioAutenticado
        );

        if (libreta.getEstado() != EstadoLibreta.GENERADA) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "La libreta todavía no está disponible");
        }

        var matriculas = matriculaRepository
                .buscarPorEstudianteYAnioId(
                        idEstudiante,
                        idAnioAcademico,
                        EstadoMatricula.CONFIRMADA
                );

        if (matriculas.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe matrícula confirmada para este año");
        }

        var matricula = matriculas.get(0);

        if (usuarioAutenticado instanceof Docente tutor) {
            matricula = matriculas.stream()
                    .filter(m -> m.getSeccion().getTutor() != null
                            && tutor.getId().equals(
                            m.getSeccion().getTutor().getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "No eres tutor de la sección del estudiante"));
        }

        var seccion = matricula.getSeccion();
        var estudiante = libreta.getEstudiante();
        var tutorSeccion = seccion.getTutor();

        var notas = notaCompetenciaRepository
                .buscarNotasParaLibreta(
                        idEstudiante,
                        bimestre.getId()
                );

        var notasResponse = notas.stream()
                .map(nota -> new LibretaNotasDetalleResponse.NotaPorCompetencia(
                        nota.getCompetencia().getCurso().getNombre(),
                        nota.getCompetencia().getNombre(),
                        nota.getValor(),
                        nota.getDescripcion(),
                        nota.getConclusionDescriptiva()
                ))
                .toList();

        long inasistencias = 0;
        long tardanzas = 0;

        var conteos = asistenciaEstudianteRepository.contarPorEstado(
                idEstudiante,
                bimestre.getFechaInicio(),
                bimestre.getFechaFin(),
                null
        );

        for (Object[] fila : conteos) {
            EstadoAsistencia estado = (EstadoAsistencia) fila[0];
            long cantidad = ((Number) fila[1]).longValue();

            if (estado == EstadoAsistencia.FALTA) {
                inasistencias += cantidad;
            } else if (estado == EstadoAsistencia.TARDANZA) {
                tardanzas += cantidad;
            }
        }

        String nombreEstudiante = estudiante.getNombre()
                + " " + estudiante.getApellidoPaterno()
                + (estudiante.getApellidoMaterno() == null
                ? ""
                : " " + estudiante.getApellidoMaterno());

        String nombreTutor = tutorSeccion == null
                ? "No asignado"
                : tutorSeccion.getNombre()
                  + " " + tutorSeccion.getApellidoPaterno();

        return new LibretaNotasDetalleResponse(
                libreta.getId(),
                idEstudiante,
                bimestre.getAnioAcademico().getAnio(),
                bimestre.getNombre().name(),
                libreta.getFechaGeneracion(),
                libreta.getComentariosFinalesTutor(),
                notasResponse,
                nombreEstudiante,
                seccion.getNombre(),
                nombreTutor,
                nombreInstitucion,
                inasistencias,
                tardanzas
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


    /**
     * HU-EVAL-06: Obtiene las calificaciones, conclusiones
     * descriptivas y asistencia para la vista previa.
     */
    @Override
    @Transactional(readOnly = true)
    public VistaPreviaLibretaNotasResponse obtenerVistaPrevia(
            Long idEstudiante,
            Long idBimestre,
            Usuario usuarioAutenticado) {

        Docente tutor = exigirTutor(usuarioAutenticado);

        var contexto = validadorGeneracionLibreta.validar(
                idEstudiante,
                idBimestre,
                tutor
        );

        var estudiante = contexto.estudiante();
        var bimestre = contexto.bimestre();
        var seccion = contexto.matricula().getSeccion();

        var notasResponse = contexto.notas().stream()
                .map(nota -> new LibretaNotasDetalleResponse.NotaPorCompetencia(
                        nota.getCompetencia().getCurso().getNombre(),
                        nota.getCompetencia().getNombre(),
                        nota.getValor(),
                        nota.getDescripcion(),
                        nota.getConclusionDescriptiva()
                ))
                .toList();

        long inasistencias = 0;
        long tardanzas = 0;

        var conteos = asistenciaEstudianteRepository.contarPorEstado(
                idEstudiante,
                bimestre.getFechaInicio(),
                bimestre.getFechaFin(),
                null
        );

        for (Object[] fila : conteos) {
            EstadoAsistencia estado = (EstadoAsistencia) fila[0];
            long cantidad = ((Number) fila[1]).longValue();

            if (estado == EstadoAsistencia.FALTA) {
                inasistencias += cantidad;
            } else if (estado == EstadoAsistencia.TARDANZA) {
                tardanzas += cantidad;
            }
        }

        return new VistaPreviaLibretaNotasResponse(
                estudiante.getId(),
                estudiante.getNombre() + " "
                        + estudiante.getApellidoPaterno(),
                seccion.getId(),
                seccion.getNombre(),
                tutor.getNombre() + " " + tutor.getApellidoPaterno(),
                bimestre.getId(),
                bimestre.getNombre().name(),
                bimestre.getAnioAcademico().getAnio(),
                nombreInstitucion,
                inasistencias,
                tardanzas,
                notasResponse
        );
    }



    /**
     * HU-EVAL-06: Consolida las notas del bimestre y
     * registra la libreta como generada.
     */
    @Override
    @Transactional
    public LibretaNotasResumenResponse generarLibreta(
            Long idEstudiante,
            GenerarLibretaNotasRequest request,
            Usuario usuarioAutenticado) {

        Docente tutor = exigirTutor(usuarioAutenticado);

        if (request == null || request.idBimestre() == null
                || request.comentariosFinalesTutor() == null
                || request.comentariosFinalesTutor().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el bimestre y los comentarios del tutor."
            );
        }

        String comentarios = request.comentariosFinalesTutor().trim();

        if (comentarios.length() > 2000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los comentarios no pueden superar los 2000 caracteres."
            );
        }

        var contexto = validadorGeneracionLibreta.validar(
                idEstudiante,
                request.idBimestre(),
                tutor
        );

        LibretaNotas libreta = new LibretaNotas();

        libreta.setEstudiante(contexto.estudiante());
        libreta.setBimestre(contexto.bimestre());
        libreta.setFechaGeneracion(LocalDate.now());
        libreta.setEstado(EstadoLibreta.GENERADA);
        libreta.setComentariosFinalesTutor(comentarios);

        LibretaNotas guardada = libretaNotasRepository.saveAndFlush(libreta);

        return new LibretaNotasResumenResponse(
                guardada.getId(),
                guardada.getEstudiante().getId(),
                guardada.getBimestre().getAnioAcademico().getAnio(),
                guardada.getBimestre().getNombre().name(),
                guardada.getFechaGeneracion(),
                guardada.getComentariosFinalesTutor()
        );
    }



    /**
     * Verifica que el usuario autenticado sea docente.
     * La asignación como tutor se valida posteriormente.
     */
    private Docente exigirTutor(Usuario usuarioAutenticado) {

        if (usuarioAutenticado == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado"
            );
        }

        if (!(usuarioAutenticado instanceof Docente tutor)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo los docentes tutores pueden generar libretas."
            );
        }

        return tutor;
    }


}
