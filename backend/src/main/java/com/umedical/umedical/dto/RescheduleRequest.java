package com.umedical.umedical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class RescheduleRequest {

    @Schema(description = "Nueva fecha (yyyy-MM-dd)", example = "2026-11-09")
    @NotNull(message = "La fecha es obligatoria")
    @FutureOrPresent(message = "La fecha no puede estar en el pasado")
    private LocalDate appointmentDate;

    @Schema(description = "Nueva hora, de un horario disponible (HH:mm:ss)", example = "09:00:00")
    @NotNull(message = "La hora es obligatoria")
    private LocalTime appointmentTime;

    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalTime appointmentTime) { this.appointmentTime = appointmentTime; }
}