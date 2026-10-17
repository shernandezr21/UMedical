package com.umedical.umedical.dto;

import java.time.LocalDate;

public class PatientProfileResponse {

    private Long patientId;
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate birthDate;
    private String address;
    private String bloodType;

    public PatientProfileResponse(Long patientId, Long userId, String username, String email,
                                  String firstName, String lastName, String phone,
                                  LocalDate birthDate, String address, String bloodType) {
        this.patientId = patientId;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.birthDate = birthDate;
        this.address = address;
        this.bloodType = bloodType;
    }

    public Long getPatientId() { return patientId; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getAddress() { return address; }
    public String getBloodType() { return bloodType; }
}