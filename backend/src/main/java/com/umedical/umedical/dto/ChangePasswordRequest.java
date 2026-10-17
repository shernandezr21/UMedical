package com.umedical.umedical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @Schema(example = "Clave12345")
    @NotBlank(message = "La contrasena actual es obligatoria")
    private String currentPassword;

    @Schema(example = "ClaveNueva123")
    @NotBlank(message = "La contrasena nueva es obligatoria")
    @Size(min = 8, max = 72, message = "La contrasena nueva debe tener entre 8 y 72 caracteres")
    private String newPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}