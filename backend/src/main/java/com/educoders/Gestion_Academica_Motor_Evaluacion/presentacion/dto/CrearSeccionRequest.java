package com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO utilizado para registrar una sección académica.
 *
 * Recibe el nombre de la sección, el grado y el año académico.
 * La capacidad máxima se establece automáticamente en 30 estudiantes
 * y la asignación del tutor se gestiona mediante otra funcionalidad.
 */
public class CrearSeccionRequest {

    @NotBlank(message = "El nombre de la sección es obligatorio")
    @Pattern(
            regexp = "^[A-Za-z]$",
            message = "El nombre de la sección debe ser una sola letra de la A a la Z"
    )
    private String nombre;

    @NotNull(message = "El ID del grado es obligatorio")
    private Long gradoId;

    @NotNull(message = "El ID del año académico es obligatorio")
    private Long anioAcademicoId;

    public CrearSeccionRequest() {
    }

    public CrearSeccionRequest(String nombre, Long gradoId, Long anioAcademicoId) {
        this.nombre = nombre;
        this.gradoId = gradoId;
        this.anioAcademicoId = anioAcademicoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getGradoId() {
        return gradoId;
    }

    public void setGradoId(Long gradoId) {
        this.gradoId = gradoId;
    }

    public Long getAnioAcademicoId() {
        return anioAcademicoId;
    }

    public void setAnioAcademicoId(Long anioAcademicoId) {
        this.anioAcademicoId = anioAcademicoId;
    }
}