package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.dominio.Aula;
import com.educoders.Gestion_Matriz_Horarios.persistencia.AulaRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.AulaRequest;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.AulaResponse;
import com.educoders.Infraestructura_Servicios.exception.ConflictoException;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AulaServiceImpl implements AulaService {

    private final AulaRepository repo;

    public AulaServiceImpl(AulaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public AulaResponse crear(AulaRequest request) {
        if (repo.existsByNombre(request.getNombre())) {
            throw new ConflictoException("Ya existe un aula con el nombre: " + request.getNombre());
        }
        Aula aula = new Aula();
        aula.setNombre(request.getNombre());
        aula.setCapacidad(request.getCapacidad());
        return mapear(repo.save(aula));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AulaResponse> listar() {
        return repo.findAll().stream().map(this::mapear).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AulaResponse obtener(Long id) {
        Aula aula = repo.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("aula", id));
        return mapear(aula);
    }

    @Override
    @Transactional
    public AulaResponse actualizar(Long id, AulaRequest request) {
        Aula aula = repo.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("aula", id));
        if (!aula.getNombre().equals(request.getNombre()) && repo.existsByNombre(request.getNombre())) {
            throw new ConflictoException("Ya existe un aula con el nombre: " + request.getNombre());
        }
        aula.setNombre(request.getNombre());
        aula.setCapacidad(request.getCapacidad());
        return mapear(repo.save(aula));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Aula aula = repo.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("aula", id));
        repo.delete(aula);
    }

    private AulaResponse mapear(Aula a) {
        AulaResponse r = new AulaResponse();
        r.setId(a.getId());
        r.setNombre(a.getNombre());
        r.setCapacidad(a.getCapacidad());
        return r;
    }
}