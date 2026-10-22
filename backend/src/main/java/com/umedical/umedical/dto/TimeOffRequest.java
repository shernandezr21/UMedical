package com.umedical.umedical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class TimeOffRequest {

    @Schema(description = "Primer dia no disponible (yyyy-MM-dd)", example = "2026-12-21")
    @NotNull(message = "La fecha inicial es obligatoria")
    private LocalDate startDate;

    @Schema(description = "Ultimo dia no disponible (yyyy-MM-dd)", example = "2026-12-27")
    @NotNull(message = "La fecha final es obligatoria")
    private LocalDate endDate;

    @Schema(description = "Motivo (opcional; obligatorio si se cancelan citas, porque lo ven los pacientes)", example = "Vacaciones")
    @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
    private String reason;

    @Schema(description = "true = cancelar las citas pendientes que haya en esas fechas", example = "false")
    private Boolean cancelExisting;

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Boolean getCancelExisting() { return cancelExisting; }
    public void setCancelExisting(Boolean cancelExisting) { this.cancelExisting = cancelExisting; }
}