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

    /**
     * Reemplaza el estado completo de bloques de una sección en un solo paso.
     * Los bloques con id se actualizan, los que no tienen id se crean y los
     * existentes que no aparecen en la lista se eliminan.
     */
    GuardadoMasivoBloquesResponse guardarMasivo(Long seccionId, GuardarBloquesSeccionRequest request);
}