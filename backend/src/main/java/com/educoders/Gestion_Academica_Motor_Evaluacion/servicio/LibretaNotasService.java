
package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.LibretaNotasResumenResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.LibretaNotasDetalleResponse;
import com.educoders.Seguridad_Auditoria.dominio.Usuario;

import java.util.List;

public interface LibretaNotasService {

    // HU-EVAL-04 y HU-EVAL-05:
    // Listar las libretas generadas de un estudiante
    // correspondientes a un año académico.
    List<LibretaNotasResumenResponse> listarLibretas(
            Long idEstudiante,
            Long idAnioAcademico,
            Usuario usuarioAutenticado
    );

    // HU-EVAL-04 y HU-EVAL-05:
    // Consultar el contenido detallado de una libreta.
    LibretaNotasDetalleResponse obtenerDetalleLibreta(
            Long idLibreta,
            Usuario usuarioAutenticado
    );

    // HU-EVAL-04 y HU-EVAL-05:
    // Descargar una libreta de notas en formato PDF.
    byte[] descargarLibretaPdf(
            Long idLibreta,
            Usuario usuarioAutenticado
    );
}
