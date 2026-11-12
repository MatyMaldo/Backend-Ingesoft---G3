package com.educoders.Seguridad_Auditoria.dominio;

import com.educoders.Seguridad_Auditoria.persistencia.AuditoriaRepository;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU-SAT-04, Esc. 1: los registros de auditoría no se pueden modificar ni eliminar. */
class AuditoriaInmutableTest {

    @Test
    void lasEntidadesSonInmutables() {
        assertTrue(Auditoria.class.isAnnotationPresent(Immutable.class));
        assertTrue(DetalleAuditoria.class.isAnnotationPresent(Immutable.class));
    }

    @Test
    void elCicloDeVidaRechazaModificarYEliminar() {
        Auditoria auditoria = new Auditoria();
        DetalleAuditoria detalle = new DetalleAuditoria("campo", "a", "b");

        assertThrows(IllegalStateException.class, auditoria::rechazarModificacion);
        assertThrows(IllegalStateException.class, auditoria::rechazarEliminacion);
        assertThrows(IllegalStateException.class, detalle::rechazarModificacion);
        assertThrows(IllegalStateException.class, detalle::rechazarEliminacion);

        assertTrue(anotado(Auditoria.class, "rechazarModificacion", PreUpdate.class));
        assertTrue(anotado(Auditoria.class, "rechazarEliminacion", PreRemove.class));
        assertTrue(anotado(DetalleAuditoria.class, "rechazarModificacion", PreUpdate.class));
        assertTrue(anotado(DetalleAuditoria.class, "rechazarEliminacion", PreRemove.class));
    }

    @Test
    void elRepositorioNoExponeOperacionesDeBorrado() {
        assertFalse(JpaRepository.class.isAssignableFrom(AuditoriaRepository.class));

        List<String> metodos = Arrays.stream(AuditoriaRepository.class.getMethods()).map(Method::getName).toList();
        assertTrue(metodos.stream().noneMatch(nombre -> nombre.startsWith("delete") || nombre.startsWith("remove")),
                "El repositorio de auditoría no debe ofrecer borrado: " + metodos);
    }

    private static boolean anotado(Class<?> tipo, String metodo, Class<? extends java.lang.annotation.Annotation> anotacion) {
        try {
            return tipo.getDeclaredMethod(metodo).isAnnotationPresent(anotacion);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
