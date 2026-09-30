package com.umedical.umedical.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class AvailableSlotResponse {

    private LocalDate date;
    private LocalTime time;

    public AvailableSlotResponse(LocalDate date, LocalTime time) {
        this.date = date;
        this.time = time;
    }

    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
}