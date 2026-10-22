package com.umedical.umedical.dto;

import java.time.LocalDate;

public class DoctorPatientResponse {

    private Long patientId;
    private String fullName;
    private String email;
    private String phone;
    private long totalAppointments;
    private long completedAppointments;
    private LocalDate lastCompletedDate;

    public DoctorPatientResponse(Long patientId, String fullName, String email, String phone,
                                 long totalAppointments, long completedAppointments, LocalDate lastCompletedDate) {
        this.patientId = patientId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.totalAppointments = totalAppointments;
        this.completedAppointments = completedAppointments;
        this.lastCompletedDate = lastCompletedDate;
    }

    public Long getPatientId() { return patientId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public long getTotalAppointments() { return totalAppointments; }
    public long getCompletedAppointments() { return completedAppointments; }
    public LocalDate getLastCompletedDate() { return lastCompletedDate; }
}