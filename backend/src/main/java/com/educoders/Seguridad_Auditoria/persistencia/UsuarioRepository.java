package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.dominio.Usuario;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    @Query("SELECT u FROM Usuario u WHERE TYPE(u) = :tipoUsuario")
    Page<Usuario> findByTipoUsuario(String tipoUsuario, Pageable pageable);
}