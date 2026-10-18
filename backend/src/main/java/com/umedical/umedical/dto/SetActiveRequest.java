package com.umedical.umedical.dto;

import jakarta.validation.constraints.NotNull;

public class SetActiveRequest {

    @NotNull(message = "El campo active es obligatorio")
    private Boolean active;

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}