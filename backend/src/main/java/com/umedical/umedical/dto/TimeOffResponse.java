package com.umedical.umedical.dto;

import java.time.LocalDate;

public class TimeOffResponse {

    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private int cancelledAppointments;

    public TimeOffResponse(Long id, LocalDate startDate, LocalDate endDate, String reason, int cancelledAppointments) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.cancelledAppointments = cancelledAppointments;
    }

    public Long getId() { return id; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public int getCancelledAppointments() { return cancelledAppointments; }
}