package com.educoders.Seguridad_Auditoria.dominio;

import jakarta.persistence.*;

/** Docente del sistema. */
@Entity
@Table(name = "docentes")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("DOCENTE")
public class Docente extends Usuario {
}