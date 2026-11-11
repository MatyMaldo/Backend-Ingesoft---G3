package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.controladores;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.AnioAcademicoResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ContextoDocenteResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.PeriodoMatriculaEstadoResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.VacantesAnioResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.AnioAcademicoService;
import com.educoders.Gestion_Academica_Motor_Evaluacion.servicio.GradoSeccionService;
import com.educoders.Infraestructura_Servicios.dto.RespuestaApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/anios-academicos")
public class AnioAcademicoController {

    private final AnioAcademicoService anioAcademicoService;
    private final GradoSeccionService gradoSeccionService;

    public AnioAcademicoController(AnioAcademicoService anioAcademicoService,
                                   GradoSeccionService gradoSeccionService) {
        this.anioAcademicoService = anioAcademicoService;
        this.gradoSeccionService = gradoSeccionService;
    }

    @GetMapping
    public ResponseEntity<RespuestaApiResponse<List<AnioAcademicoResponse>>> listarAniosAcademicos() {
        List<AnioAcademicoResponse> response = anioAcademicoService.listarAniosAcademicos();
        return ResponseEntity.ok(RespuestaApiResponse.crearExito(response));
    }

    @GetMapping("/con-periodo-matricula-vigente")
    public ResponseEntity<RespuestaApiResponse<AnioAcademicoResponse>> obtenerAnioAcademicoConPeriodoMatriculaVigente() {
        Optional<AnioAcademicoResponse> response = anioAcademicoService.obtenerAnioAcademicoConPeriodoMatriculaVigente();
        if (response.isPresent()) {
            return ResponseEntity.ok(RespuestaApiResponse.crearExito(response.get()));
        } else {
            return ResponseEntity.ok(RespuestaApiResponse.crearError("No existe un año académico futuro con período de matrícula vigente."));
        }
    }

    @GetMapping("/{anioId}/periodo-matricula")
    public ResponseEntity<RespuestaApiResponse<PeriodoMatriculaEstadoResponse>>
    obtenerPeriodoMatricula(@PathVariable Long anioId) {

        PeriodoMatriculaEstadoResponse response =
                anioAcademicoService.obtenerPeriodoMatricula(anioId);

        return ResponseEntity.ok(
                RespuestaApiResponse.crearExito(response)
        );
    }

    @GetMapping("/{anioId}/bimestres")
    public ResponseEntity<RespuestaApiResponse<List<ContextoDocenteResponse.BimestreInfo>>>
    listarBimestres(@PathVariable Long anioId) {

        List<ContextoDocenteResponse.BimestreInfo> response =
                anioAcademicoService.listarBimestres(anioId);

        return ResponseEntity.ok(
                RespuestaApiResponse.crearExito(response)
        );
    }

    @GetMapping("/{anioId}/vacantes")
    public ResponseEntity<RespuestaApiResponse<VacantesAnioResponse>>
    consultarVacantesAnio(@PathVariable Long anioId) {

        VacantesAnioResponse response =
                gradoSeccionService.consultarVacantesAnio(anioId);

        return ResponseEntity.ok(
                RespuestaApiResponse.crearExito(response)
        );
    }

}
