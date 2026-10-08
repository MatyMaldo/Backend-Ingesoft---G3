package com.educoders.Seguridad_Auditoria.dominio;

import jakarta.persistence.*;

/** Administrador del sistema. */
@Entity
@Table(name = "administradores")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("ADMINISTRADOR")
public class Administrador extends Usuario {
}