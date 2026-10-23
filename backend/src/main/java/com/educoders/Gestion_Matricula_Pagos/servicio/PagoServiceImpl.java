package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Matricula_Pagos.dominio.CompromisoPago;
import com.educoders.Gestion_Matricula_Pagos.dominio.Pago;
import com.educoders.Gestion_Matricula_Pagos.persistencia.CompromisoPagoRepository;
import com.educoders.Gestion_Matricula_Pagos.persistencia.PagoRepository;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.*;
import com.educoders.Infraestructura_Servicios.enums.EstadoCompromisoPago;
import com.educoders.Infraestructura_Servicios.enums.EstadoMatricula;
import com.educoders.Infraestructura_Servicios.enums.EstadoPago;
import com.educoders.Infraestructura_Servicios.enums.TipoCompromiso;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Infraestructura_Servicios.servicio.ServicioAlmacenamientoArchivo;
import com.educoders.Infraestructura_Servicios.servicio.ServicioNotificacionEmail;
import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final CompromisoPagoRepository compromisoPagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioAlmacenamientoArchivo servicioAlmacenamientoArchivo;
    private final ServicioNotificacionEmail servicioNotificacionEmail;

    public PagoServiceImpl(PagoRepository pagoRepository,
                           CompromisoPagoRepository compromisoPagoRepository,
                           UsuarioRepository usuarioRepository,
                           ServicioAlmacenamientoArchivo servicioAlmacenamientoArchivo,
                           ServicioNotificacionEmail servicioNotificacionEmail) {
        this.pagoRepository = pagoRepository;
        this.compromisoPagoRepository = compromisoPagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioAlmacenamientoArchivo = servicioAlmacenamientoArchivo;
        this.servicioNotificacionEmail = servicioNotificacionEmail;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoApoderadoResponse> listarPagosPorCompromisoYApoderado(Long apoderadoId, Long compromisoId) {
        CompromisoPago compromiso = compromisoPagoRepository.findById(compromisoId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Compromiso de Pago", compromisoId));

        // Regla de seguridad: Garantizar que el compromiso pertenezca a un estudiante a cargo de este apoderado
        Long apoderadoAsociadoId = compromiso.getMatricula().getEstudiante().getApoderado().getId();
        if (!apoderadoAsociadoId.equals(apoderadoId)) {
            throw new ReglaNegocioException("El compromiso de pago no pertenece a ningún estudiante a su cargo.");
        }

        return pagoRepository.findByCompromisoPagoId(compromisoId)
                .stream()
                .map(this::mapToPagoApoderadoResponse)
                .toList();
    }

    @Override
    @Transactional
    public PagoApoderadoResponse registrarPagoApoderado(RegistrarPagoRequest request) {
        // 1. Validar existencia del compromiso
        CompromisoPago compromiso = compromisoPagoRepository.findById(request.compromisoId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Compromiso de Pago", request.compromisoId()));

        // 2. Validar que el compromiso pertenezca a un estudiante a cargo del apoderado solicitante
        var apoderado = compromiso.getMatricula().getEstudiante().getApoderado();
        if (!apoderado.getId().equals(request.apoderadoId())) {
            throw new ReglaNegocioException("El compromiso de pago no pertenece a ningún estudiante a su cargo.");
        }

        // 3. Validar que no exista un pago en revisión (PENDIENTE) para el mismo compromiso
        if (pagoRepository.existsByCompromisoPagoIdAndEstado(request.compromisoId(), EstadoPago.PENDIENTE)) {
            throw new ReglaNegocioException("Ya existe una solicitud de pago pendiente de validación para este compromiso.");
        }

        // 4. Guardar archivo en Cloudinary (aplica validaciones de PDF y max 5MB)
        String evidenciaUrl = servicioAlmacenamientoArchivo.guardarArchivo(request.comprobante(), "comprobantes/pagos");

        // 5. Instanciar mediante el Builder y persistir
        Pago pago = Pago.builder()
                .fechaRegistro(LocalDate.now())
                .montoPagado(request.montoPagado())
                .evidenciaUrl(evidenciaUrl)
                .estado(EstadoPago.PENDIENTE)
                .compromisoPago(compromiso)
                .apoderadoPagador(apoderado)
                .build();

        Pago pagoGuardado = pagoRepository.save(pago);

        // 6. Mapear y retornar la respuesta
        return mapToPagoApoderadoResponse(pagoGuardado);
    }

    /*
        HU-GEMAP-08: Listado de pagos para admin
    */
    @Override
    @Transactional(readOnly = true)
    public Page<PagoAdminListResponse> listarPagosAdmin(
            Long anioAcademicoId,
            Long gradoId,
            Long seccionId,
            Long estudianteId,
            EstadoPago estado,
            TipoCompromiso tipoCompromiso,
            Pageable pageable) {

        return pagoRepository.listarPagosFiltrados(
                anioAcademicoId,
                gradoId,
                seccionId,
                estudianteId,
                estado,
                tipoCompromiso,
                pageable
        ).map(PagoAdminListResponse::fromEntity);
    }

    /*
        HU-GEMAP-08: Detalle del pago(del listado para el admin)
    */
    @Override
    @Transactional(readOnly = true)
    public PagoAdminDetailResponse obtenerDetallePagoAdmin(Long pagoId) {
        Pago pago = pagoRepository.findByIdConDetalle(pagoId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pago", pagoId));

        return PagoAdminDetailResponse.fromEntity(pago);
    }

    /*
        HU-GEMAP-08: Validación de pago por el Administrador
    */
    @Override
    @Transactional
    public PagoAdminDetailResponse validarPagoAdmin(Long pagoId, ValidarPagoRequest request) {
        // Step 1: Obtener el pago con su detalle o lanzar excepción
        Pago pago = pagoRepository.findByIdConDetalle(pagoId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pago", pagoId));

        // Step 2: Validar que el pago esté en estado PENDIENTE (Inmutabilidad del pago)
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new ReglaNegocioException("El pago ya ha sido procesado previamente y es inmutable.");
        }

        // Step 3: Obtener el compromiso y matrícula asociados
        var compromiso = pago.getCompromisoPago();
        var matricula = compromiso.getMatricula();

        // Step 4: Validar que la matrícula o el compromiso no estén cancelados
        if (matricula.getEstado() == EstadoMatricula.CANCELADA || compromiso.getEstado() == EstadoCompromisoPago.CANCELADO) {
            throw new ReglaNegocioException("No se puede validar el pago porque el compromiso o la matrícula se encuentra CANCELADA.");
        }

        // Step 5: Validar que el usuario validador exista y sea explícitamente un Administrador
        Usuario usuarioValidador = usuarioRepository.findById(request.validadorId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario Validador", request.validadorId()));

        if (!(usuarioValidador instanceof Administrador validador)) {
            throw new ReglaNegocioException("El usuario especificado no tiene el rol de Administrador para validar pagos.");
        }

        // Step 6: Realizar la transición de estados en las entidades
        pago.setEstado(EstadoPago.VALIDADO);
        pago.setUsuarioValidador(validador);

        compromiso.setEstado(EstadoCompromisoPago.PAGADO);

        if (compromiso.getTipoCompromiso() == TipoCompromiso.MATRICULA) {
            matricula.setEstado(EstadoMatricula.CONFIRMADA);
        }

        // Step 7: Persistir los cambios del pago y entidades relacionadas
        Pago pagoGuardado = pagoRepository.save(pago);

        // Step 8: Notificar al apoderado mediante el servicio de email
        var apoderado = pago.getApoderadoPagador();
        var estudiante = matricula.getEstudiante();

        servicioNotificacionEmail.enviarCorreoValidacionPago(
                apoderado.getEmail(),
                apoderado.getNombre() + " " + apoderado.getApellidoPaterno(),
                estudiante.getNombre() + " " + estudiante.getApellidoPaterno(),
                compromiso.getTipoCompromiso().name(),
                pago.getMontoPagado()
        );

        // Step 9: Mapear y retornar la respuesta DTO actualizada
        return PagoAdminDetailResponse.fromEntity(pagoGuardado);
    }

    /*
        HU-GEMAP-08: Rechazo de pago por el Administrador
    */
    @Override
    @Transactional
    public PagoAdminDetailResponse rechazarPagoAdmin(Long pagoId, RechazarPagoRequest request) {
        // Step 1: Obtener el pago con su detalle o lanzar excepción
        Pago pago = pagoRepository.findByIdConDetalle(pagoId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pago", pagoId));

        // Step 2: Validar que el pago esté en estado PENDIENTE (Inmutabilidad del pago)
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new ReglaNegocioException("El pago ya ha sido procesado previamente y es inmutable.");
        }

        // Step 3: Obtener el compromiso y matrícula asociados
        var compromiso = pago.getCompromisoPago();
        var matricula = compromiso.getMatricula();

        // Step 4: Validar que la matrícula o el compromiso no estén cancelados
        if (matricula.getEstado() == EstadoMatricula.CANCELADA || compromiso.getEstado() == EstadoCompromisoPago.CANCELADO) {
            throw new ReglaNegocioException("No se puede rechazar el pago porque el compromiso o la matrícula se encuentra CANCELADA.");
        }

        // Step 5: Validar que el usuario validador exista y sea explícitamente un Administrador
        Usuario usuarioValidador = usuarioRepository.findById(request.validadorId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario Validador", request.validadorId()));

        if (!(usuarioValidador instanceof Administrador validador)) {
            throw new ReglaNegocioException("El usuario especificado no tiene el rol de Administrador para rechazar pagos.");
        }

        /*
        *  Ejemplo de uso de auditoria: FASE DE CAPTURA DE DATOS ANTES DE CAMBIOS
        *   // FASE DE CAPTURA DE DATOS PREVIOS
            String estadoPagoAnt = pago.getEstado().name();
            String estadoCompromisoAnt = compromiso.getEstado().name();
            String estadoMatriculaAnt = matricula.getEstado().name();
        * */

        // Step 6: Realizar la transición de estados en las entidades e ingresar motivo
        pago.setEstado(EstadoPago.RECHAZADO);
        pago.setMotivo(request.motivo());
        pago.setUsuarioValidador(validador);

        compromiso.setEstado(EstadoCompromisoPago.PENDIENTE);

        if (compromiso.getTipoCompromiso() == TipoCompromiso.MATRICULA) {
            matricula.setEstado(EstadoMatricula.PENDIENTE);
        }

        // Step 7: Persistir los cambios del pago y entidades relacionadas
        Pago pagoGuardado = pagoRepository.save(pago);

        // Step 8: Notificar al apoderado vía email indicando el motivo de rechazo
        var apoderado = pago.getApoderadoPagador();
        var estudiante = matricula.getEstudiante();

        servicioNotificacionEmail.enviarCorreoRechazoPago(
                apoderado.getEmail(),
                apoderado.getNombre() + " " + apoderado.getApellidoPaterno(),
                estudiante.getNombre() + " " + estudiante.getApellidoPaterno(),
                compromiso.getTipoCompromiso().name(),
                request.motivo()
        );

        /*
         *   Ejemplo de uso de auditoria: FASE DE REGISTRO DE AUDITORIA DE LOS CAMBIOS DE ESTADOS
        // FASE DE REGISTRO DE AUDITORÍA (Garantizada contra fallos y aislada)
        auditoriaService.registrarAuditoria(new RegistrarAuditoriaRequestDTO(
                validador.getId(),
                CategoriaAuditoria.PAGOS,
                TipoAccionAuditoria.CAMBIO_ESTADO,
                "Pago",
                pagoGuardado.getId(),
                "Rechazo de pago por el administrador",
                List.of(
                        new DetalleAuditoriaDTO("estadoPago", estadoPagoAnt, pagoGuardado.getEstado().name()),
                        new DetalleAuditoriaDTO("estadoCompromiso", estadoCompromisoAnt, compromiso.getEstado().name()),
                        new DetalleAuditoriaDTO("estadoMatricula", estadoMatriculaAnt, matricula.getEstado().name())
                )
        ));
        */

        // Step 9: Mapear y retornar la respuesta DTO actualizada
        return PagoAdminDetailResponse.fromEntity(pagoGuardado);
    }

    private PagoApoderadoResponse mapToPagoApoderadoResponse(Pago p) {
        return new PagoApoderadoResponse(
                p.getId(),
                p.getFechaRegistro(),
                p.getMontoPagado(),
                p.getEvidenciaUrl(),
                p.getEstado(),
                p.getMotivo(),
                p.getCompromisoPago().getId(),
                p.getApoderadoPagador().getId()
        );
    }
}