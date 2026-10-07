package com.educoders.usuarios.entity;

import jakarta.persistence.*;

/**
 * Entidad que representa a un administrador del sistema EduCoders.
 * Hereda de Usuario y representa un rol con privilegios de administración.
 */
@Entity
@Table(name = "administradores")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("ADMINISTRADOR")
public class Administrador extends Usuario {
}
