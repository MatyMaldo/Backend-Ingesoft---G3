package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.AulaRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.AulaResponse;
import java.util.List;

public interface AulaService {
    AulaResponse crear(AulaRequest request);
    List<AulaResponse> listar();
    AulaResponse obtener(Long id);
    AulaResponse actualizar(Long id, AulaRequest request);
    void eliminar(Long id);
}