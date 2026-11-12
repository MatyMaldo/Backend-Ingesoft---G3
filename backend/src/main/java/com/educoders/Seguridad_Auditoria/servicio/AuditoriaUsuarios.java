package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.enums.CategoriaAuditoria;
import com.educoders.Infraestructura_Servicios.enums.TipoAccionAuditoria;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import com.educoders.Seguridad_Auditoria.presentacion.dto.DetalleAuditoriaDTO;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarAuditoriaRequestDTO;
import org.hibernate.Hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Arma los registros de auditoría de HU-SAT-04, categoría Usuarios: alta, edición, desactivación y reactivación
 * de usuarios y eventos de sesión. Nunca incluye contraseñas, hashes ni códigos de verificación.
 */
public final class AuditoriaUsuarios {

    public static final String ENTIDAD_USUARIO = "Usuario";
    public static final String ENTIDAD_SESION = "Sesion";

    public static final String REGISTRO = "Registro de usuario";
    public static final String EDICION = "Edición de usuario";
    public static final String DESACTIVACION = "Desactivación de usuario";
    public static final String REACTIVACION = "Reactivación de usuario";
    public static final String INICIO_SESION = "Inicio de sesión";
    public static final String INTENTO_FALLIDO = "Intento fallido de inicio de sesión";
    public static final String CIERRE_SESION = "Cierre de sesión";

    public static final String SIN_SESION = "Sin sesión";
    public static final String SESION_INICIADA = "Sesión iniciada";

    private static final String ACTIVO = "Activo";
    private static final String INACTIVO = "Inactivo";

    private AuditoriaUsuarios() {
    }

    /** Alta de un usuario: el valor anterior queda vacío y el posterior lista los datos con que se creó. */
    public static RegistrarAuditoriaRequestDTO registro(Usuario usuario) {
        List<DetalleAuditoriaDTO> detalles = new ArrayList<>();
        agregarAlta(detalles, "Nombre", usuario.getNombre());
        agregarAlta(detalles, "Apellido paterno", usuario.getApellidoPaterno());
        agregarAlta(detalles, "Apellido materno", usuario.getApellidoMaterno());
        agregarAlta(detalles, "DNI", usuario.getDni());
        agregarAlta(detalles, "Correo electrónico", usuario.getEmail());
        agregarAlta(detalles, "Teléfono", usuario.getTelefono());
        agregarAlta(detalles, "Rol", etiquetaRol(usuario));
        agregarAlta(detalles, "Estado", usuario.isActivo() ? ACTIVO : INACTIVO);

        Object real = Hibernate.unproxy(usuario);
        if (real instanceof Apoderado apoderado) {
            agregarAlta(detalles, "Edad", apoderado.getEdad() == null ? null : String.valueOf(apoderado.getEdad()));
        } else if (real instanceof Estudiante estudiante) {
            agregarAlta(detalles, "Código", estudiante.getCodigo());
            agregarAlta(detalles, "Fecha de nacimiento",
                    estudiante.getFechaNacimiento() == null ? null : estudiante.getFechaNacimiento().toString());
            if (estudiante.getApoderado() != null) {
                agregarAlta(detalles, "Apoderado", nombreCompleto(estudiante.getApoderado()));
            }
        }

        return usuarioEvento(usuario, TipoAccionAuditoria.CREAR, REGISTRO, detalles);
    }

    /** Foto de los datos editables de un usuario, para comparar antes y después de una edición. */
    public static DatosEditables datosEditables(Usuario usuario) {
        return new DatosEditables(usuario.getNombre(), usuario.getApellidoPaterno(), usuario.getApellidoMaterno(),
                usuario.getDni(), usuario.getEmail(), usuario.getTelefono());
    }

    /**
     * Edición de un usuario: un único registro con los campos que cambiaron (valor antes y después), o
     * {@code null} si el guardado no cambió ningún dato.
     */
    public static RegistrarAuditoriaRequestDTO edicion(Usuario usuario, DatosEditables antes, DatosEditables despues) {
        List<DetalleAuditoriaDTO> detalles = new ArrayList<>();
        agregarCambio(detalles, "Nombre", antes.nombre(), despues.nombre());
        agregarCambio(detalles, "Apellido paterno", antes.apellidoPaterno(), despues.apellidoPaterno());
        agregarCambio(detalles, "Apellido materno", antes.apellidoMaterno(), despues.apellidoMaterno());
        agregarCambio(detalles, "DNI", antes.dni(), despues.dni());
        agregarCambio(detalles, "Correo electrónico", antes.email(), despues.email());
        agregarCambio(detalles, "Teléfono", antes.telefono(), despues.telefono());

        if (detalles.isEmpty()) {
            return null;
        }
        return usuarioEvento(usuario, TipoAccionAuditoria.ACTUALIZAR, EDICION, detalles);
    }

    /** Desactivación ({@code activo = false}) o reactivación ({@code activo = true}) de una cuenta. */
    public static RegistrarAuditoriaRequestDTO cambioDeEstado(Usuario usuario, boolean activo) {
        DetalleAuditoriaDTO estado = new DetalleAuditoriaDTO(
                "Estado", activo ? INACTIVO : ACTIVO, activo ? ACTIVO : INACTIVO);
        return usuarioEvento(usuario, TipoAccionAuditoria.CAMBIO_ESTADO,
                activo ? REACTIVACION : DESACTIVACION, List.of(estado));
    }

    /** Inicio de sesión exitoso del titular de la cuenta. */
    public static RegistrarAuditoriaRequestDTO inicioDeSesion(Usuario titular) {
        return eventoDeSesion(titular.getId(), INICIO_SESION, SIN_SESION, SESION_INICIADA);
    }

    /**
     * Intento fallido de inicio de sesión. {@code titular} es la cuenta a la que corresponde el correo ingresado, o
     * {@code null} si no corresponde a ninguna (autor "No determinado").
     */
    public static RegistrarAuditoriaRequestDTO intentoFallido(Usuario titular) {
        return eventoDeSesion(titular == null ? null : titular.getId(), INTENTO_FALLIDO, SIN_SESION, SIN_SESION);
    }

    /** Cierre de sesión voluntario del titular de la cuenta. */
    public static RegistrarAuditoriaRequestDTO cierreDeSesion(Long usuarioId) {
        return eventoDeSesion(usuarioId, CIERRE_SESION, SESION_INICIADA, SIN_SESION);
    }

    /** Datos de un usuario que el administrador puede editar (HU-SAT-02). */
    public record DatosEditables(String nombre, String apellidoPaterno, String apellidoMaterno,
                                 String dni, String email, String telefono) {
    }

    private static RegistrarAuditoriaRequestDTO usuarioEvento(Usuario usuario, TipoAccionAuditoria accion,
                                                              String descripcion, List<DetalleAuditoriaDTO> detalles) {
        // El autor es el administrador autenticado: lo resuelve el servicio de auditoría desde el token.
        return new RegistrarAuditoriaRequestDTO(null, CategoriaAuditoria.USUARIO, accion,
                ENTIDAD_USUARIO, usuario.getId(), descripcion, detalles);
    }

    private static RegistrarAuditoriaRequestDTO eventoDeSesion(Long autorId, String descripcion,
                                                               String antes, String despues) {
        return new RegistrarAuditoriaRequestDTO(autorId, CategoriaAuditoria.USUARIO, TipoAccionAuditoria.LOGIN,
                ENTIDAD_SESION, autorId, descripcion, List.of(new DetalleAuditoriaDTO("Sesión", antes, despues)))
                .conAutorFijo();
    }

    private static void agregarAlta(List<DetalleAuditoriaDTO> detalles, String campo, String valor) {
        if (valor != null && !valor.isBlank()) {
            detalles.add(new DetalleAuditoriaDTO(campo, null, valor));
        }
    }

    private static void agregarCambio(List<DetalleAuditoriaDTO> detalles, String campo, String antes, String despues) {
        if (!Objects.equals(vacioANulo(antes), vacioANulo(despues))) {
            detalles.add(new DetalleAuditoriaDTO(campo, antes, despues));
        }
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private static String etiquetaRol(Usuario usuario) {
        Object real = Hibernate.unproxy(usuario);
        if (real instanceof Docente) {
            return "Docente";
        }
        if (real instanceof Apoderado) {
            return "Apoderado";
        }
        if (real instanceof Estudiante) {
            return "Estudiante";
        }
        return "Administrador";
    }

    private static String nombreCompleto(Usuario usuario) {
        return String.join(" ", usuario.getNombre(), usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno() == null ? "" : usuario.getApellidoMaterno()).trim();
    }
}
