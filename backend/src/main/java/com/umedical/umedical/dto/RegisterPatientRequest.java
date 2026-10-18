package com.umedical.umedical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class RegisterPatientRequest {

    @Schema(example = "maria.gomez")
    @NotBlank(message = "El username es obligatorio")
    @Size(min = 4, max = 50, message = "El username debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El username solo puede tener letras, numeros, punto, guion y guion bajo")
    private String username;

    @Schema(example = "maria.gomez@correo.com")
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es valido")
    @Size(max = 100, message = "El email no puede superar 100 caracteres")
    private String email;

    @Schema(example = "Clave12345")
    @NotBlank(message = "El password es obligatorio")
    @Size(min = 8, max = 72, message = "El password debe tener entre 8 y 72 caracteres")
    private String password;

    @Schema(example = "Maria")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    private String firstName;

    @Schema(example = "Gomez")
    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 80, message = "El apellido no puede superar 80 caracteres")
    private String lastName;

    @Schema(example = "5555-0100")
    @Size(max = 20, message = "El telefono no puede superar 20 caracteres")
    private String phone;

    @Schema(description = "Fecha de nacimiento (yyyy-MM-dd)", example = "1998-05-20")
    @Past(message = "La fecha de nacimiento debe estar en el pasado")
    private LocalDate birthDate;

    @Schema(example = "Calle de prueba 123")
    @Size(max = 255, message = "La direccion no puede superar 255 caracteres")
    private String address;

    @Schema(description = "A, B, AB u O seguido de + o -", example = "O+")
    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Tipo de sangre invalido (ej. O+, A-, AB+)")
    private String bloodType;

    @Schema(description = "Token de Cloudflare Turnstile. Solo se exige si el servidor tiene el captcha activado.")
    private String captchaToken;

    public String getCaptchaToken() { return captchaToken; }
    public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }

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
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
}