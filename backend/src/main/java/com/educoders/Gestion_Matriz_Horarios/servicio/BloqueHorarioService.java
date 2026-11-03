package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.dominio.BloqueHorario;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.*;
import java.util.List;

public interface BloqueHorarioService {
    BloqueHorarioResponse crear(CrearBloqueRequest request);
    BloqueHorarioResponse actualizar(Long id, ActualizarBloqueRequest request);
    void eliminar(Long id);
    List<BloqueHorarioResponse> listar(Long gradoId, Long seccionId, Long anioId, Long docenteId);
    BloqueHorarioResponse obtener(Long id);
}