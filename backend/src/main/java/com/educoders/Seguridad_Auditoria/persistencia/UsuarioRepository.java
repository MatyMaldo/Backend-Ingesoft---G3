package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByDni(String dni);

    Optional<Usuario> findByEmail(String email);

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    /** Filtros de listado. */
    Page<Usuario> findByActivo(boolean activo, Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE TYPE(u) = :clase")
    Page<Usuario> findByTipoUsuario(Class<?> clase, Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE TYPE(u) = :clase AND u.activo = :activo")
    Page<Usuario> findByTipoUsuarioAndActivo(Class<?> clase, boolean activo, Pageable pageable);

    @Query("""
       SELECT u
       FROM Usuario u
       WHERE u.dni = :dni
       AND TYPE(u) = :tipoUsuario
       """)
    Optional<Usuario> findByDniAndTipoUsuario(
            @Param("dni") String dni,
            @Param("tipoUsuario") Class<? extends Usuario> tipoUsuario
    );

    @Query("""
       SELECT COUNT(u) > 0
       FROM Usuario u
       WHERE u.dni = :dni
       AND TYPE(u) = :tipoUsuario
       AND u.id <> :id
       """)
    boolean existsByDniAndTipoUsuarioAndIdNot(
            @Param("dni") String dni,
            @Param("tipoUsuario") Class<? extends Usuario> tipoUsuario,
            @Param("id") Long id
    );

    @Query("""
   SELECT COUNT(u) > 0
   FROM Usuario u
   WHERE u.dni = :dni
   AND TYPE(u) = :tipoUsuario
   """)
    boolean existsByDniAndTipoUsuario(
            @Param("dni") String dni,
            @Param("tipoUsuario") Class<? extends Usuario> tipoUsuario
    );
}