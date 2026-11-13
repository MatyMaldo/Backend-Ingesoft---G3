package com.educoders.Seguridad_Auditoria.dominio;

import com.educoders.Infraestructura_Servicios.enums.TipoError;
import com.educoders.Seguridad_Auditoria.persistencia.LogErrorRepository;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU-SAT-06, Esc. 5: los registros del log de errores no se pueden modificar ni eliminar. */
class LogErrorInmutableTest {

    @Test
    void laEntidadEsInmutableYRechazaModificarYEliminar() throws NoSuchMethodException {
        assertTrue(LogError.class.isAnnotationPresent(Immutable.class));

        LogError logError = new LogError();
        assertThrows(IllegalStateException.class, logError::rechazarModificacion);
        assertThrows(IllegalStateException.class, logError::rechazarEliminacion);
        assertTrue(LogError.class.getDeclaredMethod("rechazarModificacion").isAnnotationPresent(PreUpdate.class));
        assertTrue(LogError.class.getDeclaredMethod("rechazarEliminacion").isAnnotationPresent(PreRemove.class));
    }

    @Test
    void elRepositorioNoExponeOperacionesDeBorrado() {
        assertFalse(JpaRepository.class.isAssignableFrom(LogErrorRepository.class));

        List<String> metodos = Arrays.stream(LogErrorRepository.class.getMethods()).map(Method::getName).toList();
        assertTrue(metodos.stream().noneMatch(n -> n.startsWith("delete") || n.startsWith("remove")),
                "El repositorio del log de errores no debe ofrecer borrado: " + metodos);
    }

    @Test
    void elTipoDeErrorEsUnaListaCerradaDeTresValores() {
        assertEquals(List.of("Excepción no controlada", "Proceso automático", "Notificación por correo"),
                Arrays.stream(TipoError.values()).map(TipoError::getEtiqueta).toList());
    }

    @Test
    void elConvertidorGuardaLaEtiquetaYTolerarValoresAntiguos() {
        TipoErrorConverter conversor = new TipoErrorConverter();

        assertEquals("Proceso automático", conversor.convertToDatabaseColumn(TipoError.PROCESO_AUTOMATICO));
        assertEquals(TipoError.NOTIFICACION_CORREO, conversor.convertToEntityAttribute("Notificación por correo"));
        assertEquals(TipoError.EXCEPCION_NO_CONTROLADA, conversor.convertToEntityAttribute("ErrorInterno"));
        assertEquals(TipoError.EXCEPCION_NO_CONTROLADA, conversor.convertToEntityAttribute("Recurso no encontrado"));
        assertEquals(null, conversor.convertToEntityAttribute(null));
    }
}
