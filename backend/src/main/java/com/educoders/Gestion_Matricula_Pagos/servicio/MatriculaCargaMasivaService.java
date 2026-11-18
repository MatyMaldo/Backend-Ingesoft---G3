package com.educoders.Gestion_Matricula_Pagos.servicio;

import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ConfirmarCargaMasivaMatriculasRequest;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.ConfirmarCargaMasivaMatriculasResponse;
import com.educoders.Gestion_Matricula_Pagos.presentacion.dto.PreviewCargaMasivaMatriculasResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Plantilla, exportación y carga masiva de matrículas.
 * Todas las operaciones son exclusivas del administrador.
 */
public interface MatriculaCargaMasivaService {

    /** Plantilla vacía en CSV o XLSX. */
    byte[] generarPlantilla(String formato);

    /** Matrículas PENDIENTE y CONFIRMADA del año, en formato de importación. */
    byte[] exportar(Long anioId, String formato);

    /**
     * Valida el archivo sin persistir nada. Devuelve las filas clasificadas y las
     * secciones disponibles del año destino para que el frontend pueda corregir.
     */
    PreviewCargaMasivaMatriculasResponse preview(MultipartFile archivo, Long anioAcademicoId);

    /**
     * Revalida todas las filas y crea las matrículas PENDIENTE en una sola
     * transacción. Si una fila falla, no se crea ninguna.
     */
    ConfirmarCargaMasivaMatriculasResponse confirmar(ConfirmarCargaMasivaMatriculasRequest request);
}
