package com.umedical.umedical.dto;

public class DoctorDetailResponse {

    private Long doctorId;
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Long specialtyId;
    private String specialtyName;
    private String licenseNumber;
    private String biography;
    private boolean active;

    public DoctorDetailResponse(Long doctorId, Long userId, String username, String firstName, String lastName,
                                String email, String phone, Long specialtyId, String specialtyName,
                                String licenseNumber, String biography, boolean active) {
        this.doctorId = doctorId;
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.specialtyId = specialtyId;
        this.specialtyName = specialtyName;
        this.licenseNumber = licenseNumber;
        this.biography = biography;
        this.active = active;
    }

    public Long getDoctorId() { return doctorId; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Long getSpecialtyId() { return specialtyId; }
    public String getSpecialtyName() { return specialtyName; }
    public String getLicenseNumber() { return licenseNumber; }
    public String getBiography() { return biography; }
    public boolean isActive() { return active; }
}