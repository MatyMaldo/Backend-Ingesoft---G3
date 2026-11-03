package com.educoders.Gestion_Matriz_Horarios.servicio;

import com.educoders.Gestion_Matriz_Horarios.dominio.SesionClase;
import com.educoders.Gestion_Matriz_Horarios.persistencia.SesionClaseRepository;
import com.educoders.Gestion_Matriz_Horarios.presentacion.dto.SesionClaseResponse;
import com.educoders.Infraestructura_Servicios.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SesionClaseServiceImpl implements SesionClaseService {

    private final SesionClaseRepository repo;

    public SesionClaseServiceImpl(SesionClaseRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionClaseResponse> listarPorSeccion(Long seccionId, java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin) {
        return repo.findByBloqueHorarioAsignacionCursoSeccionIdAndFechaBetween(seccionId, fechaInicio, fechaFin)
                .stream().map(this::mapear).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SesionClaseResponse obtener(Long id) {
        SesionClase s = repo.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("sesión de clase", id));
        return mapear(s);
    }

    private SesionClaseResponse mapear(SesionClase s) {
        SesionClaseResponse r = new SesionClaseResponse();
        r.setId(s.getId());
        r.setFecha(s.getFecha());
        r.setHoraInicio(s.getHoraInicio());
        r.setHoraFin(s.getHoraFin());
        r.setEstado(s.getEstado());
        r.setBloqueHorarioId(s.getBloqueHorario().getId());
        r.setDiaSemana(s.getBloqueHorario().getDiaSemana());
        r.setAulaId(s.getBloqueHorario().getAula().getId());
        r.setAulaNombre(s.getBloqueHorario().getAula().getNombre());
        r.setCursoId(s.getBloqueHorario().getAsignacionCurso().getCurso().getId());
        r.setCursoNombre(s.getBloqueHorario().getAsignacionCurso().getCurso().getNombre());
        r.setDocenteId(s.getBloqueHorario().getAsignacionCurso().getDocente().getId());
        r.setDocenteNombre(s.getBloqueHorario().getAsignacionCurso().getDocente().getNombre() + " " + s.getBloqueHorario().getAsignacionCurso().getDocente().getApellidoPaterno());
        r.setSeccionId(s.getBloqueHorario().getAsignacionCurso().getSeccion().getId());
        r.setSeccionNombre(s.getBloqueHorario().getAsignacionCurso().getSeccion().getNombre());
        return r;
    }
}