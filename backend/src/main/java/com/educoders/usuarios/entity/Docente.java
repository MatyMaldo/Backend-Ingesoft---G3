package com.educoders.usuarios.entity;

import jakarta.persistence.*;

/**
 * Entidad que representa a un docente del sistema EduCoders.
 * Hereda de Usuario y representa un rol con responsabilidades académicas.
 */
@Entity
@Table(name = "docentes")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("DOCENTE")
public class Docente extends Usuario {
}
