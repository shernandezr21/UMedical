package com.umedical.umedical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Alta de un paciente por la secretaria. El usuario y la contrasena temporal los genera el servidor. */
public class CreatePatientByStaffRequest {

    @Schema(example = "pedro.lopez@correo.com")
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es valido")
    @Size(max = 100, message = "El email no puede superar 100 caracteres")
    private String email;

    @Schema(example = "Pedro")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    private String firstName;

    @Schema(example = "Lopez")
    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 80, message = "El apellido no puede superar 80 caracteres")
    private String lastName;

    @Schema(example = "5555-0300")
    @Size(max = 20, message = "El telefono no puede superar 20 caracteres")
    private String phone;

    @Schema(description = "yyyy-MM-dd", example = "1990-03-15")
    @Past(message = "La fecha de nacimiento debe estar en el pasado")
    private LocalDate birthDate;

    @Size(max = 255, message = "La direccion no puede superar 255 caracteres")
    private String address;

    @Schema(description = "A, B, AB u O seguido de + o -", example = "B+")
    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Tipo de sangre invalido (ej. O+, A-, AB+)")
    private String bloodType;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
}