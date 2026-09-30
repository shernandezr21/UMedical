package com.umedical.umedical.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentRequest {

    @NotNull(message = "El doctorId es obligatorio")
    private Long doctorId;

    @NotNull(message = "La fecha es obligatoria")
    @Future(message = "La fecha debe ser en el futuro")
    private LocalDate appointmentDate;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime appointmentTime;

    private String reason;

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalTime appointmentTime) { this.appointmentTime = appointmentTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}