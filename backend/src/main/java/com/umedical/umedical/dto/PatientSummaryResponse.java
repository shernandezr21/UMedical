package com.umedical.umedical.dto;

import java.time.LocalDate;

public class PatientSummaryResponse {

    private Long patientId;
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String bloodType;

    public PatientSummaryResponse(Long patientId, Long userId, String username, String fullName,
                                  String email, String phone, LocalDate birthDate, String bloodType) {
        this.patientId = patientId;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
        this.bloodType = bloodType;
    }

    public Long getPatientId() { return patientId; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getBloodType() { return bloodType; }
}