package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.presentacion.dto.ActualizarUsuarioRequest;
import com.educoders.Seguridad_Auditoria.presentacion.dto.UsuarioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    Page<UsuarioResponse> listar(String tipoUsuario, Boolean activo, Pageable pageable);

    UsuarioResponse obtener(Long idUsuario);

    UsuarioResponse actualizar(Long idUsuario, ActualizarUsuarioRequest solicitud);

    UsuarioResponse desactivar(Long idUsuario);

    UsuarioResponse reactivar(Long idUsuario);
}