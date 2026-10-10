package com.umedical.umedical.dto;

import java.time.LocalTime;

public class ScheduleResponse {

    private Long id;
    private Long doctorId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes;
    private boolean active;

    public ScheduleResponse(Long id, Long doctorId, Integer dayOfWeek, LocalTime startTime,
                             LocalTime endTime, Integer slotDurationMinutes, boolean active) {
        this.id = id;
        this.doctorId = doctorId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotDurationMinutes = slotDurationMinutes;
        this.active = active;
    }

    public Long getId() { return id; }
    public Long getDoctorId() { return doctorId; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Integer getSlotDurationMinutes() { return slotDurationMinutes; }
    public boolean isActive() { return active; }
}