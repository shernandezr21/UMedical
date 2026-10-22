package com.umedical.umedical.entity.enumns;

//Esto nos ayuda a limitar el estatus de una cita a solo tres opciones: PENDIENTE, COMPLETADA o CANCELADA. 
// Esto es útil para mantener la integridad de los datos y evitar errores al asignar un estado inválido a una cita.

public enum AppointmentStatus {
    PENDIENTE,
    COMPLETADA,
    CANCELADA,
    /** El paciente no se presento a una cita que ya paso. */
    NO_ASISTIO
}
