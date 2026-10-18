package com.umedical.umedical.dto;

import java.time.LocalDate;

public class DayAvailabilityResponse {

    private LocalDate date;
    private int availableSlots;

    public DayAvailabilityResponse(LocalDate date, int availableSlots) {
        this.date = date;
        this.availableSlots = availableSlots;
    }

    public LocalDate getDate() { return date; }
    public int getAvailableSlots() { return availableSlots; }
}