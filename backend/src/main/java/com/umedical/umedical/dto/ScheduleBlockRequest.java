package com.umedical.umedical.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** Un bloque de horario semanal, sin doctor asociado todavia. */
public class ScheduleBlockRequest {

    @NotNull(message = "El dia de la semana es obligatorio")
    @Min(value = 1, message = "dayOfWeek debe ser entre 1 (Lunes) y 7 (Domingo)")
    @Max(value = 7, message = "dayOfWeek debe ser entre 1 (Lunes) y 7 (Domingo)")
    private Integer dayOfWeek;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime endTime;

    @NotNull(message = "La duracion del slot es obligatoria")
    @Min(value = 5, message = "La duracion minima es 5 minutos")
    private Integer slotDurationMinutes;

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getSlotDurationMinutes() { return slotDurationMinutes; }
    public void setSlotDurationMinutes(Integer slotDurationMinutes) { this.slotDurationMinutes = slotDurationMinutes; }
}