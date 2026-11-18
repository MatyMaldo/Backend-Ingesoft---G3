package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    boolean existsByCodigo(String codigo);

    /** Devuelve, de la lista dada, los códigos de estudiante que ya existen. Reemplaza N {@code existsByCodigo}. */
    @Query("SELECT e.codigo FROM Estudiante e WHERE e.codigo IN :codigos")
    List<String> findCodigosExistentes(@Param("codigos") Collection<String> codigos);

    boolean existsByIdAndApoderadoId(Long estudianteId, Long apoderadoId);

    Optional<Estudiante> findByEmail(String email);

    Optional<Estudiante> findByDni(String dni);

    /** Estudiantes que coinciden con los DNI indicados. Se usa en la carga masiva. */
    @Query("SELECT e FROM Estudiante e WHERE e.dni IN :dnis")
    List<Estudiante> buscarPorDnis(@Param("dnis") Collection<String> dnis);

    /** Estudiantes que coinciden con los códigos indicados. Se usa en la carga masiva. */
    @Query("SELECT e FROM Estudiante e WHERE e.codigo IN :codigos")
    List<Estudiante> buscarPorCodigos(@Param("codigos") Collection<String> codigos);

    java.util.List<Estudiante> findByApoderadoIdOrderByIdAsc(Long apoderadoId);

}