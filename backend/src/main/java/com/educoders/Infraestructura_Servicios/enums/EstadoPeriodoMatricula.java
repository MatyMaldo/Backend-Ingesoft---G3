package com.educoders.Infraestructura_Servicios.enums;

public enum EstadoPeriodoMatricula {
    /** El periodo de matrícula todavía no inicia. */
    SIN_INICIAR,
    /** La fecha actual está dentro del rango del periodo. */
    VIGENTE,
    /** El periodo de matrícula ya terminó. */
    FINALIZADO
}
