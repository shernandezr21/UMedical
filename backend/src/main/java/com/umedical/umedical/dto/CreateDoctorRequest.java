package com.umedical.umedical.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.List;

public class CreateDoctorRequest {

    @NotBlank(message = "El username es obligatorio")
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es valido")
    private String email;

    @NotBlank(message = "El password es obligatorio")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    private String phone;

    @NotNull(message = "La especialidad es obligatoria")
    private Long specialtyId;

    @NotBlank(message = "El numero de colegiado/licencia es obligatorio")
    private String licenseNumber;

    private String biography;

    @Valid
    private List<ScheduleBlockRequest> schedules;

    public List<ScheduleBlockRequest> getSchedules() { return schedules; }
    public void setSchedules(List<ScheduleBlockRequest> schedules) { this.schedules = schedules; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Long getSpecialtyId() { return specialtyId; }
    public void setSpecialtyId(Long specialtyId) { this.specialtyId = specialtyId; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }
}