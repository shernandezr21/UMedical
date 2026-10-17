package com.umedical.umedical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentRequest {

    @Schema(description = "Id del medico (GET /api/v1/doctors)", example = "1")
    @NotNull(message = "El doctorId es obligatorio")
    private Long doctorId;

    @Schema(description = "Fecha de la cita (yyyy-MM-dd)", example = "2026-11-02")
    @NotNull(message = "La fecha es obligatoria")
    @FutureOrPresent(message = "La fecha no puede estar en el pasado")
    private LocalDate appointmentDate;

    @Schema(description = "Hora de un horario disponible (HH:mm:ss), tal como lo devuelve /schedules/available", example = "08:30:00")
    @NotNull(message = "La hora es obligatoria")
    private LocalTime appointmentTime;

    @Schema(description = "Motivo de la consulta (opcional)", example = "Chequeo general")
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
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

// package com.umedical.umedical.dto;

// import jakarta.validation.constraints.Future;
// import jakarta.validation.constraints.NotNull;

// import java.time.LocalDate;
// import java.time.LocalTime;

// public class AppointmentRequest {

//     @NotNull(message = "El doctorId es obligatorio")
//     private Long doctorId;

//     @NotNull(message = "La fecha es obligatoria")
//     @Future(message = "La fecha debe ser en el futuro")
//     private LocalDate appointmentDate;

//     @NotNull(message = "La hora es obligatoria")
//     private LocalTime appointmentTime;

//     private String reason;

//     public Long getDoctorId() { return doctorId; }
//     public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
//     public LocalDate getAppointmentDate() { return appointmentDate; }
//     public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
//     public LocalTime getAppointmentTime() { return appointmentTime; }
//     public void setAppointmentTime(LocalTime appointmentTime) { this.appointmentTime = appointmentTime; }
//     public String getReason() { return reason; }
//     public void setReason(String reason) { this.reason = reason; }
// }