package com.umedical.umedical.dto;

public class DoctorResponse {

    private Long id;
    private String fullName;
    private String specialty;
    private String licenseNumber;
    private String biography;

    public DoctorResponse(Long id, String fullName, String specialty, String licenseNumber, String biography) {
        this.id = id;
        this.fullName = fullName;
        this.specialty = specialty;
        this.licenseNumber = licenseNumber;
        this.biography = biography;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getSpecialty() { return specialty; }
    public String getLicenseNumber() { return licenseNumber; }
    public String getBiography() { return biography; }
}