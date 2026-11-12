package com.educoders.Seguridad_Auditoria.persistencia;

import com.educoders.Seguridad_Auditoria.dominio.TokenRestablecimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenRestablecimientoRepository
        extends JpaRepository<TokenRestablecimiento, Long> {

    Optional<TokenRestablecimiento> findTopByUsuarioIdAndUsadoFalseOrderByFechaGeneracionDesc(
            Long usuarioId);

    Optional<TokenRestablecimiento> findTopByUsuarioIdOrderByFechaGeneracionDesc(Long usuarioId);

    List<TokenRestablecimiento> findByUsuarioIdAndUsadoFalse(Long usuarioId);
}
