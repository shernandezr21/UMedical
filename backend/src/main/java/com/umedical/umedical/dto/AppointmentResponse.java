package com.umedical.umedical.dto;

import com.umedical.umedical.entity.enumns.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentResponse {

    private Long id;
    private String doctorName;
    private String specialty;
    private String patientName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private AppointmentStatus status;
    private String reason;

    public AppointmentResponse(Long id, String doctorName, String specialty, String patientName,
                                LocalDate appointmentDate, LocalTime appointmentTime,
                                AppointmentStatus status, String reason) {
        this.id = id;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.patientName = patientName;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.status = status;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public String getDoctorName() { return doctorName; }
    public String getSpecialty() { return specialty; }
    public String getPatientName() { return patientName; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public AppointmentStatus getStatus() { return status; }
    public String getReason() { return reason; }
}