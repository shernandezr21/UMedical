package com.umedical.umedical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @Schema(description = "ROLE_ADMIN, ROLE_SECRETARY o ROLE_PATIENT (los medicos se crean desde /doctors)", example = "ROLE_SECRETARY")
    @NotBlank(message = "El rol es obligatorio")
    private String role;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 80, message = "El apellido no puede superar 80 caracteres")
    private String lastName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es valido")
    @Size(max = 100, message = "El email no puede superar 100 caracteres")
    private String email;

    @Size(max = 20, message = "El telefono no puede superar 20 caracteres")
    private String phone;

    @Schema(description = "Opcional: si se omite se genera a partir del correo")
    @Size(min = 4, max = 50, message = "El username debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El username solo puede tener letras, numeros, punto, guion y guion bajo")
    private String username;

    @Schema(description = "Opcional: si se omite se genera una contrasena temporal que se devuelve una sola vez")
    @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
    private String password;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}