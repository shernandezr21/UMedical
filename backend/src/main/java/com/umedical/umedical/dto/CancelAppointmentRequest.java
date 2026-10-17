package com.umedical.umedical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public class CancelAppointmentRequest {

    @Schema(description = "Motivo de la cancelacion (opcional)", example = "Tuve un imprevisto")
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}